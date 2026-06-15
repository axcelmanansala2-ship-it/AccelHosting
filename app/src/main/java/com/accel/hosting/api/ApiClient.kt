package com.accel.hosting.api

import android.content.Context
import com.accel.hosting.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var _service: ApiService? = null
    private var currentUrl: String = ""
    private var currentToken: String? = null

    fun init(context: Context) {
        val url = TokenManager.getServerUrl(context)
        val token = TokenManager.getToken(context)
        if (url != currentUrl || token != currentToken || _service == null) {
            currentUrl = url
            currentToken = token
            _service = build(url, token)
        }
    }

    fun reset() {
        _service = null
        currentUrl = ""
        currentToken = null
    }

    val service: ApiService
        get() = _service ?: error("ApiClient not initialized. Call init() first.")

    private fun build(baseUrl: String, token: String?): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    token?.let { addHeader("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
