package com.accel.hosting

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
        supportActionBar?.title = "Server Settings"

        binding.etServerUrl.setText(TokenManager.getServerUrl(this))

        binding.btnSave.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                Toast.makeText(this, "Enter a valid URL (http:// or https://)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            TokenManager.saveServerUrl(this, url)
            ApiClient.reset()
            Toast.makeText(this, "Server URL saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnTest.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isBlank()) { Toast.makeText(this, "Enter a URL first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            TokenManager.saveServerUrl(this, url)
            ApiClient.reset()
            ApiClient.init(this)
            binding.btnTest.isEnabled = false
            binding.tvTestResult.visibility = View.VISIBLE
            binding.tvTestResult.text = "Testing…"
            lifecycleScope.launch {
                try {
                    val result = ApiClient.service.healthz()
                    binding.tvTestResult.text = "✓ Connected — ${result["status"]}"
                    binding.tvTestResult.setTextColor(getColor(R.color.status_running))
                } catch (e: Exception) {
                    binding.tvTestResult.text = "✕ Failed: ${e.message}"
                    binding.tvTestResult.setTextColor(getColor(R.color.status_crashed))
                } finally {
                    binding.btnTest.isEnabled = true
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
