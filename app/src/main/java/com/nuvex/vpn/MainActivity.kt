package com.nuvex.vpn

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

private object Palette {
    val background = Color.rgb(7, 14, 31)
    val panel = Color.rgb(15, 27, 51)
    val raised = Color.rgb(21, 37, 67)
    val blue = Color.rgb(34, 137, 255)
    val cyan = Color.rgb(0, 213, 255)
    val primary = Color.rgb(239, 245, 255)
    val muted = Color.rgb(143, 163, 194)
    val stroke = Color.rgb(35, 60, 98)
}

private data class Server(val flag: String, val name: String, val region: String)

class MainActivity : Activity() {
    private val servers = emptyList<Server>()
    private var selected = 0
    private var serverList: LinearLayout? = null
    private var selectedLabel: TextView? = null
    private var connectButton: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Palette.background
        window.navigationBarColor = Palette.background
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.let { 0 }
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Palette.background)
            isFillViewport = true
            clipToPadding = false
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(16))
        }
        scroll.addView(root)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("◈  NUVEX", 21f, Palette.primary, true),
            LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(pill("PRIVATE BY DESIGN", Palette.raised, Palette.cyan))
        root.addView(header)

        root.addView(label("YOUR CONNECTION", 10f, Palette.cyan, true), margin(top = 10, bottom = 3))
        root.addView(label("Your privacy, your control.", 25f, Palette.primary, true), margin(bottom = 3))
        root.addView(label("A simpler, more private internet.", 12f, Palette.muted), margin(bottom = 12))

        val connection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(Palette.panel, 22, Palette.stroke)
        }
        val connectionTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val orb = TextView(this).apply {
            text = "◈"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Palette.cyan)
            background = rounded(Palette.raised, 100, Palette.stroke)
        }
        connectionTop.addView(orb, LinearLayout.LayoutParams(dp(60), dp(60)).apply { rightMargin = dp(12) })
        val statusColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("NOT CONNECTED", 14f, Palette.primary, true))
            addView(label("Automatic setup · no config files", 11f, Palette.muted), margin(top = 4))
        }
        connectionTop.addView(statusColumn, LinearLayout.LayoutParams(0, -2, 1f))
        connection.addView(connectionTop)
        connectButton = TextView(this).apply {
            text = "Connect to Nuvex"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(Palette.blue, 15, Palette.blue)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Automatic connection is not ready: Nuvex needs a verified VPN provider and live gateways first.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        connection.addView(connectButton, margin(top = 12))
        root.addView(connection, margin(bottom = 14))

        val locationHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        locationHeader.addView(label("LIVE LOCATIONS", 12f, Palette.primary, true),
            LinearLayout.LayoutParams(0, dp(28), 1f))
        locationHeader.addView(label("0 AVAILABLE", 10f, Palette.muted, true))
        root.addView(locationHeader, margin(bottom = 5))

        selectedLabel = label("No server selected", 11f, Palette.cyan, true)
        root.addView(selectedLabel!!, margin(bottom = 6))
        root.addView(label("No verified VPN provider is configured. Locations appear only after live availability is confirmed.", 11f, Palette.muted),
            margin(bottom = 8))

        serverList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(serverList)
        renderServers()

        root.addView(label("NO ADS  ·  NO TRACKERS  ·  OPEN SOURCE", 9f, Palette.muted, true)
            .apply { gravity = Gravity.CENTER; letterSpacing = 0.04f }, margin(top = 10))
    }

    private fun renderServers() {
        val list = serverList ?: return
        list.removeAllViews()
        if (servers.isEmpty()) {
            list.addView(label("Provider not configured", 14f, Palette.primary, true), margin(top = 8, bottom = 3))
            list.addView(label("Nuvex needs an authorized provider account before it can show real locations or connect.", 11f, Palette.muted), margin(bottom = 6))
            return
        }
        servers.forEachIndexed { index, server ->
            val active = index == selected
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), dp(8), dp(11), dp(8))
                background = rounded(if (active) Palette.raised else Palette.panel, 13,
                    if (active) Palette.blue else Palette.stroke)
                setOnClickListener {
                    selected = index
                    selectedLabel?.text = "Selected: ${server.name}"
                    renderServers()
                }
            }
            row.addView(label(server.flag, 20f, Palette.primary),
                LinearLayout.LayoutParams(dp(36), dp(34)).apply { gravity = Gravity.CENTER_VERTICAL })
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(server.name, 13f, Palette.primary, true))
                addView(label(server.region, 10f, Palette.muted), margin(top = 1))
            }
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label(if (active) "SELECTED" else "PLANNED", 9f,
                if (active) Palette.cyan else Palette.muted, true))
            list.addView(row, margin(bottom = 5))
        }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setLineSpacing(dp(1).toFloat(), 1f)
        }

    private fun pill(value: String, fill: Int, textColor: Int) = TextView(this).apply {
        text = value
        textSize = 8f
        letterSpacing = 0.03f
        setTextColor(textColor)
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(7), dp(9), dp(7))
        background = rounded(fill, 50, Palette.stroke)
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun margin(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
