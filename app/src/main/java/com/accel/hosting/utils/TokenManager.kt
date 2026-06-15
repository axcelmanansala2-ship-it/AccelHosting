package com.accel.hosting.utils

import android.content.Context

object TokenManager {
    private const val PREFS = "accel_prefs"
    private const val KEY_TOKEN = "token"
    const val SERVER_URL = "https://secret-vault--notjack200.replit.app"

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

    fun getServerUrl(context: Context): String = SERVER_URL
}
