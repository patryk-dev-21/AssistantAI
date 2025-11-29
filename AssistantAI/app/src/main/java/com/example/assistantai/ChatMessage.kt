package com.example.assistantai

data class ChatMessage(
    val message: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis()
)