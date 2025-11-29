package com.example.assistantai

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assistantai.databinding.ItemChatMessageBinding

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return ChatViewHolder(
            ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]

        with(holder.binding) {
            tvMessage.text = msg.message
            val params = messageContainer.layoutParams as ConstraintLayout.LayoutParams

            if (msg.isUser) {
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
                messageContainer.setCardBackgroundColor(
                    ContextCompat.getColor(
                        root.context, R.color.main_app_color
                    )
                )
                tvMessage.setTextColor(Color.WHITE)
            } else {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                messageContainer.setCardBackgroundColor(
                    ContextCompat.getColor(
                        root.context, R.color.gray_background
                    )
                )
                tvMessage.setTextColor(Color.BLACK)
            }
            messageContainer.layoutParams = params
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}