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
    val panelRaised = Color.rgb(21, 37, 67)
    val blue = Color.rgb(54, 153, 255)
    val cyan = Color.rgb(91, 214, 255)
    val primary = Color.rgb(239, 245, 255)
    val muted = Color.rgb(143, 163, 194)
    val stroke = Color.rgb(35, 60, 98)
}

private data class Server(val flag: String, val name: String, val region: String)

class MainActivity : Activity() {
    private val servers = listOf(
        Server("🇸🇬", "Singapore", "Asia Pacific"),
        Server("🇮🇳", "India", "South Asia"),
        Server("🇯🇵", "Japan", "Asia Pacific"),
        Server("🇳🇱", "Netherlands", "Europe"),
        Server("🇩🇪", "Germany", "Europe"),
        Server("🇬🇧", "United Kingdom", "Europe"),
        Server("🇺🇸", "United States", "North America"),
        Server("🇨🇦", "Canada", "North America"),
        Server("🇫🇷", "France", "Europe"),
        Server("🇦🇺", "Australia", "Oceania")
    )
    private var selected = 0
    private var serverList: LinearLayout? = null
    private var selectedLabel: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Palette.background
        window.navigationBarColor = Palette.background
        window.decorView.systemUiVisibility = 0
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Palette.background)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "◈  NUVEX"
            textSize = 21f
            letterSpacing = 0.12f
            setTextColor(Palette.primary)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        header.addView(brand, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(pill("PRIVATE BY DESIGN", Palette.panelRaised, Palette.cyan))
        root.addView(header)

        root.addView(label("YOUR CONNECTION", 11f, Palette.muted, true), margin(top = 32, bottom = 8))
        root.addView(label("Your privacy,\nyour control.", 30f, Palette.primary, true), margin(bottom = 10))
        root.addView(label("A simple home for a more private internet.", 14f, Palette.muted), margin(bottom = 22))

        val connection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(26), dp(18), dp(24))
            background = rounded(Palette.panel, 28, Palette.stroke)
        }
        val orb = TextView(this).apply {
            text = "◈"
            textSize = 44f
            gravity = Gravity.CENTER
            setTextColor(Palette.cyan)
            background = rounded(Palette.panelRaised, 100, Palette.stroke)
            elevation = dp(4).toFloat()
        }
        connection.addView(orb, LinearLayout.LayoutParams(dp(104), dp(104)).apply { bottomMargin = dp(18) })
        connection.addView(label("NOT CONNECTED", 15f, Palette.primary, true), margin(bottom = 6))
        connection.addView(label("VPN gateway not configured yet", 12f, Palette.muted), margin(bottom = 20))
        val connect = TextView(this).apply {
            text = "  Set up secure connection  "
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(Palette.blue, 18, Palette.blue)
            setPadding(dp(20), dp(15), dp(20), dp(15))
            setOnClickListener {
                Toast.makeText(this@MainActivity,
                    "VPN servers are not configured yet. No traffic has been routed.",
                    Toast.LENGTH_LONG).show()
            }
        }
        connection.addView(connect, LinearLayout.LayoutParams(-1, -2))
        root.addView(connection, margin(bottom = 24))

        val serverHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        serverHeader.addView(label("CHOOSE A LOCATION", 13f, Palette.primary, true),
            LinearLayout.LayoutParams(0, dp(32), 1f))
        serverHeader.addView(label("10 REGIONS", 11f, Palette.cyan, true))
        root.addView(serverHeader, margin(bottom = 10))

        val chosen = servers[selected]
        selectedLabel = label("${chosen.flag}   ${chosen.name}  ·  Planned location", 14f, Palette.primary, true)
        selectedLabel!!.setPadding(dp(15), dp(17), dp(15), dp(17))
        selectedLabel!!.background = rounded(Palette.panelRaised, 16, Palette.blue)
        root.addView(selectedLabel!!, margin(bottom = 8))

        val hint = label("Locations are placeholders until verified VPN gateways are connected.", 12f, Palette.muted)
        root.addView(hint, margin(bottom = 10))

        serverList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(serverList)
        renderServers()

        root.addView(label("NO ADS  ·  NO TRACKERS  ·  OPEN SOURCE", 10f, Palette.muted, true)
            .apply { gravity = Gravity.CENTER; letterSpacing = 0.08f }, margin(top = 24))
        root.addView(label("Nuvex VPN  •  v0.1.0 development build", 11f, Palette.muted)
            .apply { gravity = Gravity.CENTER }, margin(top = 8))
    }

    private fun renderServers() {
        val list = serverList ?: return
        list.removeAllViews()
        servers.forEachIndexed { index, server ->
            val active = index == selected
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(13), dp(14), dp(13))
                background = rounded(if (active) Palette.panelRaised else Palette.panel, 15,
                    if (active) Palette.blue else Palette.stroke)
                setOnClickListener {
                    selected = index
                    selectedLabel?.text = "${server.flag}   ${server.name}  ·  Planned location"
                    renderServers()
                }
            }
            val flag = TextView(this).apply { text = server.flag; textSize = 23f }
            row.addView(flag, LinearLayout.LayoutParams(dp(42), dp(38)).apply { gravity = Gravity.CENTER_VERTICAL })
            val text = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(server.name, 14f, Palette.primary, true))
                addView(label(server.region, 11f, Palette.muted), margin(top = 3))
            }
            row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label(if (active) "SELECTED" else "SOON", 10f,
                if (active) Palette.cyan else Palette.muted, true))
            list.addView(row, margin(bottom = 8))
        }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun pill(value: String, fill: Int, textColor: Int) = TextView(this).apply {
        text = value
        textSize = 9f
        letterSpacing = 0.05f
        setTextColor(textColor)
        gravity = Gravity.CENTER
        setPadding(dp(11), dp(9), dp(11), dp(9))
        background = rounded(fill, 50, Palette.stroke)
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int): GradientDrawable =
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
