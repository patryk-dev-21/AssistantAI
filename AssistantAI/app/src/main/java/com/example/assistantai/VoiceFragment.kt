package com.example.assistantai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.example.assistantai.databinding.FragmentVoiceBinding
import java.util.Locale

class VoiceFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentVoiceBinding? = null
    private val binding get() = _binding!!

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var isListening = false
    private val viewModel: AssistantViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startListening() else Toast.makeText(
                context, "Permission denied", Toast.LENGTH_SHORT
            ).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textToSpeech = TextToSpeech(context, this)

        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            setupSpeechRecognizer()
        } else {
            Toast.makeText(
                context, getString(R.string.speech_recognition_not_available), Toast.LENGTH_LONG
            ).show()
            binding.btnMic.isEnabled = false
        }

        viewModel.aiResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.tvConversation.text = it
                speakOut(it)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.tvStatus.text = getString(R.string.thinking)
            } else if (!isListening) {
                binding.tvStatus.text = getString(R.string.tap_to_speak)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.tvConversation.text = it
                speakOut(it)
            }
        }

        binding.btnMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (!isListening) startListening() else stopListening()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun getCurrentLocale(): Locale {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return when (prefs.getString("app_language", "en")) {
            "fr" -> Locale.FRANCE
            "pl" -> Locale("pl", "PL")
            else -> Locale.US
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!isAdded) return
                binding.tvStatus.text = getString(R.string.listening)
                isListening = true
                startPulseAnimation()
            }

            override fun onResults(results: Bundle?) {
                if (!isAdded) return
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val query = matches[0]
                    binding.tvConversation.text = query
                    isListening = false
                    stopUIAnimation()
                    viewModel.askQuestion(query, forVoice = true)
                }
            }

            override fun onError(error: Int) {
                if (!isAdded) return
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Error: $error"
                }
                binding.tvStatus.text = msg
                isListening = false
                stopUIAnimation()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (isAdded) binding.tvStatus.text = getString(R.string.thinking)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (!isAdded) return
        if (textToSpeech.isSpeaking) textToSpeech.stop()
        speechRecognizer.cancel()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            val locale = getCurrentLocale()
            val languageTag = locale.toLanguageTag()

            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)

            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf<String>())

            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopListening() {
        if (::speechRecognizer.isInitialized) speechRecognizer.stopListening()
        isListening = false
        stopUIAnimation()
    }

    private fun startPulseAnimation() {
        binding.viewPulse.alpha = 1f
        val pulse = ScaleAnimation(
            1f, 1.5f, 1f, 1.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        binding.viewPulse.startAnimation(pulse)
    }

    private fun stopUIAnimation() {
        binding.viewPulse.clearAnimation()
        binding.viewPulse.animate().alpha(0f).duration = 300
    }

    private fun speakOut(text: String) {
        if (!isAdded) return
        textToSpeech.language = getCurrentLocale()
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AssistantResponse")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && isAdded) {
            textToSpeech.language = getCurrentLocale()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        _binding = null
    }
}