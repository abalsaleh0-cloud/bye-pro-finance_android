package com.example.finance.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class SessionResponse(val authenticated: Boolean, val user: Map<String, Any?>? = null)
data class TransferRequest(
    val from_account_id: String,
    val to_account_id: String,
    val amount: Double,
    val fee: Double,
    val currency_code: String,
    val description: String,
    val transfer_date: String,
    val status: String
)
data class ApiEnvelope<T>(val data: T? = null, val error: String? = null)
data class AiRequest(val prompt: String)

/** Retrofit contract matching the existing BYE PRO FINANCE Web API. */
interface FinanceApi {
    @GET("api/v1/session")
    suspend fun session(@Header("Authorization") bearerToken: String): SessionResponse

    @POST("api/v1/transfers")
    suspend fun createTransfer(
        @Header("Authorization") bearerToken: String,
        @Header("Idempotency-Key") idempotencyKey: String?,
        @Body request: TransferRequest
    ): ApiEnvelope<Map<String, Any?>>

    @POST("api/v1/ai")
    suspend fun ai(
        @Header("Authorization") bearerToken: String,
        @Body request: AiRequest
    ): ApiEnvelope<Map<String, Any?>>
}
