package com.example.dailycash.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val QUOTE_URL = "https://zenquotes.io/"
    private const val CURRENCY_URL = "https://v6.exchangerate-api.com/"

    val quoteApi: QuoteApiService by lazy {
        Retrofit.Builder()
            .baseUrl(QUOTE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuoteApiService::class.java)
    }

    val currencyApi: CurrencyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(CURRENCY_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApiService::class.java)
    }

    // Keep 'instance' for backward compatibility
    val instance: QuoteApiService get() = quoteApi
}
