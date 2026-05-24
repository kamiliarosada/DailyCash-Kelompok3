package com.example.dailycash.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface QuoteApiService {
    @GET("api/random")
    suspend fun getRandomQuote(): List<QuoteResponse>
}

data class QuoteResponse(
    @SerializedName("q") val text: String,
    @SerializedName("a") val author: String
)
