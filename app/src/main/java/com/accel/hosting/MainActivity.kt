package com.accel.hosting

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accel.hosting.api.ApiClient
import com.accel.hosting.utils.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            delay(1200)
            val token = TokenManager.getToken(this@MainActivity)
            if (token.isNullOrBlank()) {
                goTo(LoginActivity::class.java)
                return@launch
            }
            ApiClient.init(this@MainActivity)
            try {
                ApiClient.service.me()
                goTo(DashboardActivity::class.java)
            } catch (e: Exception) {
                TokenManager.clearToken(this@MainActivity)
                goTo(LoginActivity::class.java)
            }
        }
    }

    private fun goTo(cls: Class<*>) {
        startActivity(Intent(this, cls))
        finish()
    }
}
