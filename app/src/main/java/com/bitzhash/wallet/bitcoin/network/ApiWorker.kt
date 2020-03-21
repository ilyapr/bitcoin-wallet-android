package com.bitzhash.wallet.bitcoin.network

import com.google.gson.GsonBuilder
import com.bitzhash.wallet.bitcoin.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit

object ApiWorker {
    private var mClient: OkHttpClient? = null
    private var mGsonConverter: GsonConverterFactory? = null

    val client: OkHttpClient
        @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
        get() {
            if (mClient == null) {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY

                 val builder = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)

                 if (BuildConfig.DEBUG) {
                     builder.addInterceptor(interceptor) // show all JSON in logCat
                 }

                mClient = builder.build()
            }

            return mClient!!
        }

    val gsonConverter: GsonConverterFactory
        get() {
            if (mGsonConverter == null) {
                mGsonConverter = GsonConverterFactory.create(GsonBuilder()
                    .setLenient().disableHtmlEscaping().create())
            }

            return mGsonConverter!!
        }
}