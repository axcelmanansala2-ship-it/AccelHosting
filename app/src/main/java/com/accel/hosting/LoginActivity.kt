package com.accel.hosting

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accel.hosting.api.ApiClient
import com.accel.hosting.databinding.ActivityLoginBinding
import com.accel.hosting.models.LoginRequest
import com.accel.hosting.utils.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        // Animate form sliding up on open
        binding.root.alpha = 0f
        binding.root.translationY = 60f
        binding.root.animate()
            .alpha(1f).translationY(0f)
            .setDuration(450)
            .setStartDelay(80)
            .start()

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun doLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)
        ApiClient.init(this)
        lifecycleScope.launch {
            try {
                val resp = ApiClient.service.login(LoginRequest(username, password))
                TokenManager.saveToken(this@LoginActivity, resp.token)
                ApiClient.reset()
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            } catch (e: Exception) {
                val msg = e.message ?: "Login failed"
                Toast.makeText(this@LoginActivity,
                    if (msg.contains("401") || msg.contains("Invalid")) "Invalid credentials"
                    else "Error: $msg",
                    Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
