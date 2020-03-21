package com.bitzhash.wallet.bitcoin.network

import retrofit2.Retrofit

object ApiService {

    private const val API_BASE_PATH = "https://www.bitstamp.net/api/"

    fun bitstampApi() = Retrofit.Builder()
        .baseUrl(API_BASE_PATH)
        .addConverterFactory(ApiWorker.gsonConverter)
        .client(ApiWorker.client)
        .build()
        .create(ExchangeApiService::class.java)
}