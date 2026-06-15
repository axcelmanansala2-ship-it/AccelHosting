package com.accel.hosting

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accel.hosting.api.ApiClient
import com.accel.hosting.databinding.ActivityBotDetailBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class BotDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBotDetailBinding
    private var botId: String = ""
    private var pollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBotDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        botId = intent.getStringExtra("bot_id") ?: ""
        val botName = intent.getStringExtra("bot_name") ?: "Bot"
        supportActionBar?.title = botName

        ApiClient.init(this)

        binding.btnStart.setOnClickListener { doAction("start") }
        binding.btnStop.setOnClickListener { doAction("stop") }
        binding.btnRestart.setOnClickListener { doAction("restart") }
        binding.btnRefreshLogs.setOnClickListener { loadLogs() }
        binding.btnClearLogs.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Logs")
                .setMessage("Clear all logs for this bot?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            ApiClient.service.clearBotLogs(botId)
                            binding.tvLogs.text = ""
                            Toast.makeText(this@BotDetailActivity, "Logs cleared", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@BotDetailActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        loadBot()
        loadLogs()
    }

    override fun onResume() {
        super.onResume()
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
                loadBot(silent = true)
                delay(5000)
            }
        }
    }

    private fun loadBot(silent: Boolean = false) {
        lifecycleScope.launch {
            try {
                val bot = ApiClient.service.getBot(botId)
                supportActionBar?.title = bot.name

                val statusText = when (bot.status) {
                    "running" -> "● Running"
                    "stopped" -> "■ Stopped"
                    "crashed", "error" -> "✕ Crashed"
                    "installing" -> "↓ Installing…"
                    else -> bot.status
                }
                binding.tvStatus.text = statusText
                val statusColor = getColor(when (bot.status) {
                    "running" -> R.color.status_running
                    "stopped" -> R.color.status_stopped
                    "crashed", "error" -> R.color.status_crashed
                    "installing" -> R.color.status_installing
                    else -> R.color.text_secondary
                })
                binding.tvStatus.setTextColor(statusColor)

                binding.tvEntryFile.text = "Entry: ${bot.entryFile}"

                val info = buildString {
                    bot.cpuPercent?.let { append("CPU: ${it.roundToInt()}%   ") }
                    bot.memMb?.let { append("RAM: ${it.roundToInt()} MB   ") }
                    bot.uptime?.let {
                        val s = it.toLong()
                        val upStr = when {
                            s < 60 -> "${s}s"
                            s < 3600 -> "${s / 60}m ${s % 60}s"
                            else -> "${s / 3600}h ${(s % 3600) / 60}m"
                        }
                        append("Uptime: $upStr")
                    }
                    if (bot.crashCount > 0) append("   Crashes: ${bot.crashCount}")
                }
                binding.tvInfo.text = info.ifBlank { "Auto-restart: ${if (bot.autoRestart) "On" else "Off"}" }

                val isRunning = bot.status == "running"
                val isBusy = bot.status == "installing"
                binding.btnStart.visibility = if (!isRunning && !isBusy) View.VISIBLE else View.GONE
                binding.btnStop.visibility = if (isRunning) View.VISIBLE else View.GONE
                binding.btnRestart.visibility = if (isRunning) View.VISIBLE else View.GONE
                binding.btnStart.isEnabled = !isBusy
            } catch (e: Exception) {
                if (!silent) Toast.makeText(this@BotDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            try {
                val resp = ApiClient.service.getBotLogs(botId)
                val logText = resp.logs.joinToString("\n")
                binding.tvLogs.text = if (logText.isBlank()) "(no logs)" else logText
                binding.scrollLogs.post { binding.scrollLogs.fullScroll(View.FOCUS_DOWN) }
            } catch (e: Exception) {
                binding.tvLogs.text = "Failed to load logs: ${e.message}"
            }
        }
    }

    private fun doAction(action: String) {
        lifecycleScope.launch {
            try {
                when (action) {
                    "start" -> ApiClient.service.startBot(botId)
                    "stop" -> ApiClient.service.stopBot(botId)
                    "restart" -> ApiClient.service.restartBot(botId)
                }
                Toast.makeText(this@BotDetailActivity,
                    "${action.replaceFirstChar { it.uppercase() }}ed successfully", Toast.LENGTH_SHORT).show()
                loadBot()
                if (action != "stop") loadLogs()
            } catch (e: Exception) {
                Toast.makeText(this@BotDetailActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bot_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_delete -> {
                AlertDialog.Builder(this)
                    .setTitle("Delete Bot")
                    .setMessage("This will permanently delete the bot and all its files. Continue?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                ApiClient.service.deleteBot(botId)
                                Toast.makeText(this@BotDetailActivity, "Bot deleted", Toast.LENGTH_SHORT).show()
                                finish()
                            } catch (e: Exception) {
                                Toast.makeText(this@BotDetailActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
