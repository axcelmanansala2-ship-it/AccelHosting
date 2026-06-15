package com.accel.hosting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accel.hosting.api.ApiClient
import com.accel.hosting.databinding.ActivityRegisterBinding
import com.accel.hosting.models.RegisterRequest
import com.accel.hosting.utils.TokenManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Account"

        // Slide in from right
        binding.root.alpha = 0f
        binding.root.translationX = 60f
        binding.root.animate()
            .alpha(1f).translationX(0f)
            .setDuration(380)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        binding.btnRegister.setOnClickListener { doRegister() }
        binding.tvLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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

    private fun doRegister() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        when {
            username.isBlank() || email.isBlank() || password.isBlank() ->
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            password.length < 6 ->
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            else -> {
                setLoading(true)
                ApiClient.init(this)
                lifecycleScope.launch {
                    try {
                        val resp = ApiClient.service.register(RegisterRequest(username, email, password))
                        TokenManager.saveToken(this@RegisterActivity, resp.token)
                        ApiClient.reset()
                        val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    } catch (e: Exception) {
                        val msg = e.message ?: "Registration failed"
                        Toast.makeText(this@RegisterActivity,
                            if (msg.contains("taken") || msg.contains("400")) "Username or email already taken"
                            else "Error: $msg",
                            Toast.LENGTH_LONG).show()
                    } finally {
                        setLoading(false)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
