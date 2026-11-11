package com.nuvex.vpn

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.wireguard.android.backend.Tunnel

private object P {
    val bg = Color.rgb(5, 8, 20); val panel = Color.rgb(15, 20, 38)
    val raised = Color.rgb(31, 25, 57); val blue = Color.rgb(153, 67, 255)
    val cyan = Color.rgb(196, 111, 255); val text = Color.rgb(245, 244, 255)
    val muted = Color.rgb(164, 166, 194); val stroke = Color.rgb(51, 43, 79)
    val green = Color.rgb(54, 239, 157); val red = Color.rgb(255, 76, 101)
}
private data class Profile(val flag: String, val name: String, val server: String, val config: String)

/** Country configs bundled in app/src/main/assets/configs/ (git-ignored, they contain private keys). */
private data class BundledServer(val file: String, val code: String, val country: String, val flag: String)
private val BUNDLED_SERVERS = listOf(
    BundledServer("us.conf", "US", "United States", "🇺🇸"),
    BundledServer("ca.conf", "CA", "Canada", "🇨🇦"),
    BundledServer("nl.conf", "NL", "Netherlands", "🇳🇱"),
    BundledServer("sg.conf", "SG", "Singapore", "🇸🇬"),
    BundledServer("ro.conf", "RO", "Romania", "🇷🇴")
)

class MainActivity : Activity() {
    private val profiles = mutableListOf<Profile>()
    private var selected = 0
    private var connected = false
    private val engine by lazy { WireGuardEngine(this) }
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var connect: TextView
    private lateinit var count: TextView
    private lateinit var selectedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = P.bg
        window.navigationBarColor = P.bg
        window.decorView.systemUiVisibility = 0
        val scroll = ScrollView(this).apply { setBackgroundColor(P.bg); isFillViewport = true; clipToPadding = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(14), dp(20), dp(26)) }
        scroll.addView(root); setContentView(scroll)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("⬡  Nuvex VPN", 25f, P.text, true), LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(pill("✦ FREE", P.raised, P.cyan)); root.addView(header)
        root.addView(label("Secure  ·  Fast  ·  Free", 13f, P.muted), margin(0, 18))
        root.addView(label("YOUR CONNECTION", 12f, P.cyan, true).apply { gravity = Gravity.CENTER }, margin(8, 4))
        root.addView(label("One tap to feel safer.", 22f, P.text, true).apply { gravity = Gravity.CENTER }, margin(0, 5))
        root.addView(label("Choose your location below.", 14f, P.muted).apply { gravity = Gravity.CENTER }, margin(0, 14))
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(12), dp(16), dp(12), dp(16)); background = rounded(P.panel, 22, P.stroke) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.gravity = Gravity.CENTER
        top.addView(TextView(this).apply { text = "⛨"; textSize = 25f; gravity = Gravity.CENTER; setTextColor(P.cyan); background = rounded(P.raised, 18, P.stroke) }, LinearLayout.LayoutParams(dp(54), dp(54)).apply { rightMargin = dp(12) })
        val statusCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = label("NOT CONNECTED", 17f, P.text, true).apply { gravity = Gravity.CENTER }
        detail = label("Pick a country, then tap Connect.", 13f, P.muted).apply { gravity = Gravity.CENTER }
        statusCol.addView(status); statusCol.addView(detail, margin(5, 0))
        top.addView(statusCol, LinearLayout.LayoutParams(0, -2, 1f)); card.addView(top, margin(0, 10))
        connect = button("⏻\nCONNECT", P.blue).apply {
            textSize = 19f; minHeight = dp(0); gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(191, 104, 255), Color.rgb(100, 39, 230))).apply { shape = GradientDrawable.OVAL; setStroke(dp(4), Color.rgb(74, 37, 119)) }
        }
        connect.setOnClickListener {
            if (connected) disconnectVpn()
            else if (profiles.isEmpty()) showStatus("NO SERVERS", "No server configs are bundled in this build.")
            else {
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    @Suppress("DEPRECATION")
                    startActivityForResult(intent, VPN_PERMISSION)
                } else connectVpn()
            }
        }
        val connectHolder = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.VERTICAL }
        connectHolder.addView(connect, LinearLayout.LayoutParams(dp(220), dp(220)))
        card.addView(connectHolder, margin(14, 8)); root.addView(card, margin(0, 14))
        val section = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        section.addView(label("SERVER LOCATIONS", 15f, P.text, true), LinearLayout.LayoutParams(0, dp(30), 1f))
        count = label("0 LOCATIONS", 12f, P.muted, true); section.addView(count); root.addView(section, margin(0, 6))
        selectedText = label("Select a location", 13f, P.cyan, true); root.addView(selectedText, margin(0, 8))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(list)
        root.addView(label("DARK NAVY  ·  NEON CYAN  ·  WIREGUARD", 9f, P.muted).apply { gravity = Gravity.CENTER; letterSpacing = 0.035f }, margin(18, 0))
        loadBundledProfiles()
        renderProfiles()
    }

    /** Reads the bundled country configs. A missing or invalid file is skipped, never faked. */
    private fun loadBundledProfiles() {
        profiles.clear()
        for (server in BUNDLED_SERVERS) {
            val raw = runCatching { assets.open("configs/" + server.file).bufferedReader().use { it.readText() } }.getOrNull() ?: continue
            if (!raw.contains("[Interface]") || !raw.contains("[Peer]") || !Regex("(?m)^\\s*PrivateKey\\s*=").containsMatchIn(raw)) continue
            val number = Regex("(?m)^\\s*#\\s*" + server.code + "-FREE#([0-9]+)").find(raw)?.groupValues?.get(1)
            profiles.add(Profile(server.flag, server.country, if (number != null) "Free server #" + number else "Free server", raw.trim()))
        }
        selected = 0
    }

    @Deprecated("Uses Activity results for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_PERMISSION) {
            if (resultCode == RESULT_OK) connectVpn() else showStatus("VPN PERMISSION DENIED", "Allow Android's VPN prompt to continue.")
        }
    }

    private fun connectVpn() {
        val profile = profiles.getOrNull(selected) ?: run { showStatus("CHOOSE A SERVER", "Select a country first."); return }
        showStatus("CONNECTING…", profile.name); connect.isEnabled = false
        Thread {
            try {
                engine.connect("nuvex", profile.config) { state ->
                    runOnUiThread {
                        if (state == Tunnel.State.UP) { connected = true; showStatus("CONNECTED", "Tunnel is up · " + profile.name); connect.text = "⏻\nDISCONNECT"; connect.isEnabled = true }
                        else if (state == Tunnel.State.DOWN && connected) { connected = false; showStatus("DISCONNECTED", "VPN tunnel stopped."); connect.text = "⏻\nCONNECT"; connect.isEnabled = true }
                    }
                }
                runOnUiThread {
                    connected = engine.currentState() == Tunnel.State.UP
                    showStatus(if (connected) "CONNECTED" else "NOT CONNECTED", if (connected) "Tunnel is up · " + profile.name else "Tunnel did not report an active state.")
                    connect.text = if (connected) "⏻\nDISCONNECT" else "⏻\nCONNECT"; connect.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread { connected = false; showStatus("CONNECTION FAILED", e.message?.take(100) ?: "Check the config and server."); connect.text = "⏻\nTRY AGAIN"; connect.isEnabled = true }
            }
        }.start()
    }

    private fun disconnectVpn() {
        connect.isEnabled = false
        Thread {
            val error = runCatching { engine.disconnect() }.exceptionOrNull()
            runOnUiThread {
                connected = false
                showStatus(if (error == null) "NOT CONNECTED" else "DISCONNECT ERROR", if (error == null) "VPN tunnel stopped." else "Please try again.")
                connect.text = "⏻\nCONNECT"; connect.isEnabled = true
            }
        }.start()
    }

    private fun showStatus(title: String, message: String) {
        status.text = title; detail.text = message
        status.setTextColor(if (title == "CONNECTED") P.green else if (title.contains("FAILED") || title.contains("ERROR") || title.contains("DENIED")) P.red else P.text)
    }

    private fun renderProfiles() {
        if (!::list.isInitialized) return
        list.removeAllViews(); count.text = profiles.size.toString() + " LOCATIONS"
        if (profiles.isEmpty()) {
            list.addView(label("No server configs in this build", 16f, P.text, true), margin(12, 4))
            list.addView(label("Add your .conf files to app/src/main/assets/configs/ and rebuild.", 13f, P.muted), margin(0, 8))
            selectedText.text = "No locations available"; return
        }
        profiles.forEachIndexed { index, profile ->
            val active = index == selected
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(11), dp(12), dp(11))
                background = rounded(if (active) P.raised else P.panel, 15, if (active) P.blue else P.stroke)
                setOnClickListener {
                    if (!connected) { selected = index; renderProfiles() }
                    else Toast.makeText(this@MainActivity, "Disconnect before changing servers.", Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(label(profile.flag, 23f, P.text), LinearLayout.LayoutParams(dp(42), dp(38)).apply { gravity = Gravity.CENTER_VERTICAL })
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(label(profile.name, 15f, P.text, true)); addView(label(profile.server, 12f, P.muted), margin(3, 0)) }
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label(if (active) "SELECTED" else "SELECT", 11f, if (active) P.cyan else P.muted, true))
            list.addView(row, margin(0, 7))
        }
        selectedText.text = "Selected: " + profiles[selected].name
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f); includeFontPadding = false
    }
    private fun pill(value: String, fill: Int, color: Int) = TextView(this).apply {
        text = value; textSize = 9f; letterSpacing = 0.04f; setTextColor(color); gravity = Gravity.CENTER
        setPadding(dp(10), dp(8), dp(10), dp(8)); background = rounded(fill, 50, P.stroke)
    }
    private fun button(value: String, fill: Int) = TextView(this).apply {
        text = value; textSize = 16f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        setPadding(dp(14), dp(14), dp(14), dp(14)); background = rounded(fill, 15, if (fill == P.blue) P.blue else P.stroke)
    }
    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply { setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke) }
    private fun margin(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    companion object { private const val VPN_PERMISSION = 4102 }
}
