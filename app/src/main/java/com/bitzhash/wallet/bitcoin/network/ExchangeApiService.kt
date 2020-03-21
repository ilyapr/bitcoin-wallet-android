package com.bitzhash.wallet.bitcoin.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Headers

data class TickerData(
    @SerializedName("timestamp") var timestamp: Int,
    @SerializedName("last") var price: Double
)

interface ExchangeApiService {
    @Headers("Content-Type: application/json")
    @GET("ticker")
    suspend fun ticker(): TickerData
}