package com.shashank.paymentappui

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Data classes for OpenAI API

data class OpenAIMessage(
    val role: String = "user",
    val content: String
)

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<OpenAIMessage>
)

data class OpenAIChoice(
    val message: OpenAIMessage
)

data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

interface OpenAIApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    fun getChatCompletion(@Body request: OpenAIRequest): Call<OpenAIResponse>
} 