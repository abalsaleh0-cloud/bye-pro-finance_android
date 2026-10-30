package com.example.finance.network

import com.example.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object FinanceApiProvider {
    val api: FinanceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.FINANCE_API_BASE_URL.ensureTrailingSlash())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(FinanceApi::class.java)
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
}
