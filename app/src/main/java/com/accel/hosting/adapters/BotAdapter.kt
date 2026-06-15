package com.accel.hosting.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.accel.hosting.R
import com.accel.hosting.models.Bot
import kotlin.math.roundToInt

class BotAdapter(
    private val onStart: (Bot) -> Unit,
    private val onStop: (Bot) -> Unit,
    private val onDetail: (Bot) -> Unit
) : ListAdapter<Bot, BotAdapter.BotViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Bot>() {
            override fun areItemsTheSame(a: Bot, b: Bot) = a.id == b.id
            override fun areContentsTheSame(a: Bot, b: Bot) = a == b
        }
    }

    inner class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusStrip: View     = view.findViewById(R.id.statusStrip)
        val tvName: TextView      = view.findViewById(R.id.tvBotName)
        val tvStatus: TextView    = view.findViewById(R.id.tvBotStatus)
        val tvInfo: TextView      = view.findViewById(R.id.tvBotInfo)
        val btnAction: ImageButton = view.findViewById(R.id.btnBotAction)
        val btnDetail: ImageButton = view.findViewById(R.id.btnBotDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bot, parent, false)
        return BotViewHolder(view)
    }

    override fun onBindViewHolder(holder: BotViewHolder, position: Int) {
        val bot = getItem(position)
        val ctx = holder.itemView.context

        holder.tvName.text = bot.name

        val (statusText, statusColorRes) = when (bot.status) {
            "running"           -> "● Running"      to R.color.status_running
            "stopped"           -> "■ Stopped"      to R.color.status_stopped
            "crashed", "error"  -> "✕ Crashed"      to R.color.status_crashed
            "installing"        -> "↓ Installing…"  to R.color.status_installing
            else                -> bot.status       to R.color.text_secondary
        }
        val statusColor = ContextCompat.getColor(ctx, statusColorRes)

        // Colored left strip
        holder.statusStrip.setBackgroundColor(statusColor)

        // Status badge — semi-transparent fill + status-color text
        holder.tvStatus.text = statusText
        holder.tvStatus.setTextColor(statusColor)
        val density = ctx.resources.displayMetrics.density
        val badge = GradientDrawable().apply {
            cornerRadius = 12f * density
            setColor(ColorUtils.setAlphaComponent(statusColor, 38))
        }
        holder.tvStatus.background = badge

        // Info line
        val infoText = buildString {
            if (bot.status == "running") {
                bot.cpuPercent?.let { append("CPU ${it.roundToInt()}%  ") }
                bot.memMb?.let { append("RAM ${it.roundToInt()} MB  ") }
                bot.uptime?.let { sec ->
                    val s = sec.toLong()
                    append(when {
                        s < 60   -> "${s}s"
                        s < 3600 -> "${s / 60}m ${s % 60}s"
                        else     -> "${s / 3600}h ${(s % 3600) / 60}m"
                    })
                }
            } else {
                append(bot.entryFile)
                if (bot.crashCount > 0)
                    append("  ✕ ${bot.crashCount} crash${if (bot.crashCount > 1) "es" else ""}")
            }
        }
        holder.tvInfo.text = infoText.ifBlank { bot.entryFile }

        // Action button — tint matches status color for "play" icon, red for stop
        val isRunning = bot.status == "running"
        holder.btnAction.setImageResource(if (isRunning) R.drawable.ic_stop else R.drawable.ic_play)
        holder.btnAction.contentDescription = if (isRunning) "Stop" else "Start"
        val actionTint = if (isRunning)
            ContextCompat.getColor(ctx, R.color.status_crashed)
        else
            ContextCompat.getColor(ctx, R.color.status_running)
        holder.btnAction.setColorFilter(actionTint)
        holder.btnAction.setOnClickListener { if (isRunning) onStop(bot) else onStart(bot) }

        holder.btnDetail.setOnClickListener { onDetail(bot) }
        holder.itemView.setOnClickListener { onDetail(bot) }
    }
}
