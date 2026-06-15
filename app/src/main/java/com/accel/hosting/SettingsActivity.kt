package com.accel.hosting

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accel.hosting.api.ApiClient
import com.accel.hosting.databinding.ActivitySettingsBinding
import com.accel.hosting.utils.TokenManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"

        // Slide in from right
        binding.root.alpha = 0f
        binding.root.translationX = 60f
        binding.root.animate()
            .alpha(1f).translationX(0f)
            .setDuration(350)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        binding.tvServerUrl.text = TokenManager.SERVER_URL
        binding.tvAppVersion.text = "v1.0"

        ApiClient.init(this)

        binding.btnTestConnection.setOnClickListener {
            binding.btnTestConnection.isEnabled = false
            binding.tvConnectionStatus.visibility = View.VISIBLE
            binding.tvConnectionStatus.text = "Testing…"
            lifecycleScope.launch {
                try {
                    val result = ApiClient.service.healthz()
                    binding.tvConnectionStatus.text = "✓ Connected — ${result["status"]}"
                    binding.tvConnectionStatus.setTextColor(getColor(R.color.status_running))
                } catch (e: Exception) {
                    binding.tvConnectionStatus.text = "✕ Unreachable: ${e.message}"
                    binding.tvConnectionStatus.setTextColor(getColor(R.color.status_crashed))
                } finally {
                    binding.btnTestConnection.isEnabled = true
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        return true
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
