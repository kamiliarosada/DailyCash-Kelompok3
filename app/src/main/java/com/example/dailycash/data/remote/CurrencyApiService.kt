package com.example.dailycash.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {
    @GET("v6/{apiKey}/pair/{from}/{to}/{amount}")
    suspend fun convertCurrency(
        @Path("apiKey") apiKey: String,
        @Path("from") from: String,
        @Path("to") to: String,
        @Path("amount") amount: Double
    ): CurrencyResponse
}

data class CurrencyResponse(
    val conversion_result: Double,
    val conversion_rate: Double
)
