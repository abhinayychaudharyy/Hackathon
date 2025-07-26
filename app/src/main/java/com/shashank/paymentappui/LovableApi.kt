package com.shashank.paymentappui

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// Data class for a search result (adjust fields as needed)
data class LovableResult(
    val title: String?,
    val description: String?
)

interface LovableApi {
    @GET("api/search") // Placeholder endpoint
    fun search(@Query("q") query: String): Call<List<LovableResult>>
} 