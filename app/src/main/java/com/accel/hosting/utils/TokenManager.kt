package com.accel.hosting.utils

import android.content.Context

object TokenManager {
    private const val PREFS = "accel_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_SERVER_URL = "server_url"
    const val DEFAULT_SERVER_URL = "https://your-server.replit.app"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun clearToken(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_TOKEN).apply()
    }

    fun saveServerUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SERVER_URL, url.trimEnd('/')).apply()
    }

    fun getServerUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
}
