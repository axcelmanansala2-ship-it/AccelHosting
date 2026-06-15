package com.accel.hosting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
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

        val logo = findViewById<View>(R.id.ivLogo)
        val appName = findViewById<View>(R.id.tvAppName)
        val tagline = findViewById<View>(R.id.tvTagline)

        logo.alpha = 0f
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f
        appName.alpha = 0f
        appName.translationY = 50f
        tagline.alpha = 0f
        tagline.translationY = 50f

        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(550)
            .setStartDelay(120)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()

        appName.animate()
            .alpha(1f).translationY(0f)
            .setDuration(420)
            .setStartDelay(400)
            .start()

        tagline.animate()
            .alpha(1f).translationY(0f)
            .setDuration(420)
            .setStartDelay(560)
            .start()

        lifecycleScope.launch {
            delay(1500)
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
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
