package com.shashank.paymentappui


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Represents the body of the POST request to the OpenAI API
data class OpenAIIAPI(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>
)

// Represents a message in the chat format
data class Message(
    val role: String = "user",  // or "assistant"
    val content: String
)
