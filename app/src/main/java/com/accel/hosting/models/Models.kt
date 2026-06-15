package com.accel.hosting.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("is_admin") val isAdmin: Boolean,
    val token: String
)

data class Bot(
    val id: String,
    val name: String,
    val status: String,
    val entryFile: String,
    val uploadedAt: String,
    val pid: Int?,
    val uptime: Double?,
    val cpuPercent: Double?,
    val memMb: Double?,
    val installedPackages: List<String>,
    val autoRestart: Boolean,
    val crashCount: Int
)

data class BotStats(
    val totalBots: Int,
    val runningBots: Int,
    val stoppedBots: Int,
    val crashedBots: Int,
    val totalCrashes: Int
)

data class LogsResponse(val logs: List<String>)

data class SimpleResponse(val success: Boolean)

data class UpdateBotRequest(
    val name: String? = null,
    val autoRestart: Boolean? = null
)
