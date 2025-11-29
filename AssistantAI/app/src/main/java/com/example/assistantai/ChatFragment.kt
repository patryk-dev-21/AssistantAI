package com.example.assistantai

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assistantai.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatMessage>()
    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatAdapter = ChatAdapter(chatList)
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }

        if (chatList.isEmpty()) {
            addMessage(getString(R.string.how_can_i_help_you_today), false)
        }

        viewModel.aiResponse.observe(viewLifecycleOwner) { response ->
            response?.let { addMessage(it, false) }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { addMessage("Error: $it", false) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.tvTypingStatus.isVisible = isLoading
            if (isLoading) {
                binding.tvTypingStatus.text = getString(R.string.thinking)
                scrollToBottom()
            }
        }

        binding.btnNewChat.setOnClickListener {
            chatList.clear()
            chatAdapter.notifyDataSetChanged()
            viewModel.clearHistory()
            Toast.makeText(context, "Conversation cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.input.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.input.text.clear()
                hideKeyboard()
                addMessage(text, true)
                viewModel.askQuestion(text, false)
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        chatAdapter.addMessage(ChatMessage(text, isUser))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (chatAdapter.itemCount > 0) {
            binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.input.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}