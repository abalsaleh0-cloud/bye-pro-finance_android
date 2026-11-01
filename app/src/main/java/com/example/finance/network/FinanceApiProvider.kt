package com.example.finance.network

import com.example.BuildConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object FinanceApiProvider {
    val api: FinanceApi by lazy {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .client(httpClient)
            .baseUrl(BuildConfig.FINANCE_API_BASE_URL.ensureTrailingSlash())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(FinanceApi::class.java)
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
}
