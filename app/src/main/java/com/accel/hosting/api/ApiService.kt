package com.accel.hosting.api

import com.accel.hosting.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(): SimpleResponse

    @GET("api/auth/me")
    suspend fun me(): AuthResponse

    @GET("api/bots")
    suspend fun listBots(): List<Bot>

    @GET("api/bots/stats")
    suspend fun getBotStats(): BotStats

    @GET("api/bots/{id}")
    suspend fun getBot(@Path("id") id: String): Bot

    @DELETE("api/bots/{id}")
    suspend fun deleteBot(@Path("id") id: String): SimpleResponse

    @POST("api/bots/{id}/start")
    suspend fun startBot(@Path("id") id: String): Bot

    @POST("api/bots/{id}/stop")
    suspend fun stopBot(@Path("id") id: String): Bot

    @POST("api/bots/{id}/restart")
    suspend fun restartBot(@Path("id") id: String): Bot

    @GET("api/bots/{id}/logs")
    suspend fun getBotLogs(@Path("id") id: String): LogsResponse

    @DELETE("api/bots/{id}/logs/clear")
    suspend fun clearBotLogs(@Path("id") id: String): SimpleResponse

    @Multipart
    @POST("api/bots/upload")
    suspend fun uploadBot(
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part
    ): Bot

    @GET("api/healthz")
    suspend fun healthz(): Map<String, String>
}
