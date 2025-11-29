package com.example.assistantai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiApiService.create()
    private val conversationHistory = mutableListOf<Content>()
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(application) }

    private val _aiResponse = MutableLiveData<String?>()
    val aiResponse: LiveData<String?> get() = _aiResponse

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun askQuestion(query: String, forVoice: Boolean = false) {
        val apiKey = prefs.getString("api_key", "")
        if (apiKey.isNullOrEmpty()) {
            _errorMessage.value = getApplication<Application>().getString(R.string.api_key_missing)
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val promptText = buildPrompt(query, forVoice, conversationHistory.isEmpty())
                val userContent = Content(role = "user", parts = listOf(Part(text = promptText)))

                val requestContents = conversationHistory.toMutableList().apply { add(userContent) }
                val requestBody = GeminiRequest(contents = requestContents)

                val response = geminiService.generateResponse(apiKey, requestBody)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (aiText != null) {
                    conversationHistory.add(userContent)
                    conversationHistory.add(
                        Content(
                            role = "model", parts = listOf(Part(text = aiText))
                        )
                    )
                    _aiResponse.value = aiText
                } else {
                    _errorMessage.value =
                        getApplication<Application>().getString(R.string.response_not_recognized)
                }
            } catch (e: Exception) {
                if (e.message?.contains("400") == true) {
                    _errorMessage.value = "Error 400: Check API Key or Request format"
                } else {
                    _errorMessage.value =
                        getApplication<Application>().getString(R.string.connection_error)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    private fun buildPrompt(query: String, forVoice: Boolean, isFirstMessage: Boolean): String {
        val sb = StringBuilder()

        if (isFirstMessage) {
            val userName = prefs.getString("user_name", "") ?: ""
            val instruction = prefs.getString("user_instruction", "") ?: ""
            if (userName.isNotEmpty()) sb.append("My name is $userName. ")
            if (instruction.isNotEmpty()) sb.append("$instruction ")
        }

        sb.append(query)

        val langCode = prefs.getString("app_language", "en") ?: "en"
        val langName = when (langCode) {
            "pl" -> "Polish"
            "fr" -> "French"
            else -> "English"
        }

        val style = if (forVoice) "Keep response concise for speech." else "Keep response concise."
        sb.append("\n[System: $style Do not use Markdown styling. Reply in $langName.]")

        return sb.toString().trim()
    }
}