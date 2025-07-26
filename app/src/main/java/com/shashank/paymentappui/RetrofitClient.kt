package com.shashank.paymentappui

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request

object RetrofitClient {
    val api: LovableApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lovable.dev/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LovableApi::class.java)
    }

    val openAiApi: OpenAIApi by lazy {
        val apiKey = "YOUR_OPENAI_API_KEY" // <-- Replace with your OpenAI API key
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val original: Request = chain.request()
                val request: Request = original.newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            })
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(OpenAIApi::class.java)
    }
} 