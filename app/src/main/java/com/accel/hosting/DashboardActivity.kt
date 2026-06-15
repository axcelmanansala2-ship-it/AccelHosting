package com.accel.hosting

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accel.hosting.adapters.BotAdapter
import com.accel.hosting.api.ApiClient
import com.accel.hosting.databinding.ActivityDashboardBinding
import com.accel.hosting.models.Bot
import com.accel.hosting.utils.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var adapter: BotAdapter
    private var pollJob: Job? = null
    private var firstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "AccelHosting"

        // FAB starts hidden — animates in after first load
        binding.fabUpload.scaleX = 0f
        binding.fabUpload.scaleY = 0f
        binding.fabUpload.alpha = 0f

        adapter = BotAdapter(
            onStart = { bot -> controlBot(bot, "start") },
            onStop = { bot -> controlBot(bot, "stop") },
            onDetail = { bot ->
                val intent = Intent(this, BotDetailActivity::class.java)
                intent.putExtra("bot_id", bot.id)
                intent.putExtra("bot_name", bot.name)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        )
        binding.recyclerBots.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }
        binding.fabUpload.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        ApiClient.init(this)
        loadData()
    }

    override fun onResume() {
        super.onResume()
        ApiClient.init(this)
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        pollJob?.cancel()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                loadData(silent = true)
                delay(5000)
            }
        }
    }

    private fun loadData(silent: Boolean = false) {
        if (!silent) binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val bots = ApiClient.service.listBots()
                val stats = ApiClient.service.getBotStats()

                binding.tvTotalBots.text = stats.totalBots.toString()
                binding.tvRunningBots.text = stats.runningBots.toString()
                binding.tvStoppedBots.text = stats.stoppedBots.toString()
                binding.tvCrashedBots.text = stats.crashedBots.toString()

                adapter.submitList(bots)
                binding.tvEmpty.visibility = if (bots.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerBots.visibility = if (bots.isEmpty()) View.GONE else View.VISIBLE

                // Animate FAB in on first successful load
                if (firstLoad) {
                    firstLoad = false
                    binding.fabUpload.animate()
                        .scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(350)
                        .setStartDelay(200)
                        .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                        .start()
                }
            } catch (e: Exception) {
                if (!silent) Toast.makeText(this@DashboardActivity, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun controlBot(bot: Bot, action: String) {
        lifecycleScope.launch {
            try {
                when (action) {
                    "start" -> ApiClient.service.startBot(bot.id)
                    "stop" -> ApiClient.service.stopBot(bot.id)
                }
                loadData(silent = true)
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                true
            }
            R.id.action_logout -> {
                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout") { _, _ -> doLogout() }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun doLogout() {
        lifecycleScope.launch {
            try { ApiClient.service.logout() } catch (_: Exception) {}
            TokenManager.clearToken(this@DashboardActivity)
            ApiClient.reset()
            val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}
