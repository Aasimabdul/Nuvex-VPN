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
import java.io.BufferedReader
import java.io.InputStreamReader

private object P {
    val bg = Color.rgb(7, 14, 31); val panel = Color.rgb(15, 27, 51)
    val raised = Color.rgb(21, 37, 67); val blue = Color.rgb(34, 137, 255)
    val cyan = Color.rgb(0, 213, 255); val text = Color.rgb(239, 245, 255)
    val muted = Color.rgb(143, 163, 194); val stroke = Color.rgb(35, 60, 98)
    val green = Color.rgb(65, 220, 160); val red = Color.rgb(255, 105, 125)
}
private data class Profile(val endpoint: String, val flag: String, val name: String, val config: String)

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
        header.addView(label("◈  NUVEX", 25f, P.text, true), LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(pill("WIREGUARD", P.raised, P.cyan)); root.addView(header)
        root.addView(label("YOUR CONNECTION", 12f, P.cyan, true), margin(14, 4))
        root.addView(label("Privacy, on your terms.", 28f, P.text, true), margin(0, 5))
        root.addView(label("Choose a server and connect securely.", 15f, P.muted), margin(0, 16))
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); background = rounded(P.panel, 22, P.stroke) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply { text = "◈"; textSize = 27f; gravity = Gravity.CENTER; setTextColor(P.cyan); background = rounded(P.raised, 18, P.stroke) }, LinearLayout.LayoutParams(dp(58), dp(58)).apply { rightMargin = dp(12) })
        val statusCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = label("NOT CONNECTED", 16f, P.text, true)
        detail = label("Import your WireGuard configs to begin.", 13f, P.muted)
        statusCol.addView(status); statusCol.addView(detail, margin(5, 0))
        top.addView(statusCol, LinearLayout.LayoutParams(0, -2, 1f)); card.addView(top)
        connect = button("CONNECT", P.blue).apply { textSize = 17f; minHeight = dp(60) }
        connect.setOnClickListener {
            if (connected) disconnectVpn()
            else if (profiles.isEmpty()) pickConfigs()
            else {
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    @Suppress("DEPRECATION")
                    startActivityForResult(intent, VPN_PERMISSION)
                } else connectVpn()
            }
        }
        val connectHolder = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.VERTICAL }; connectHolder.addView(connect, LinearLayout.LayoutParams(dp(230), -2)); card.addView(connectHolder, margin(16, 0)); root.addView(card, margin(0, 18))
        val section = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        section.addView(label("SERVER LOCATIONS", 15f, P.text, true), LinearLayout.LayoutParams(0, dp(30), 1f))
        count = label("0 IMPORTED", 12f, P.muted, true); section.addView(count); root.addView(section, margin(0, 6))
        selectedText = label("Import configs to select a location", 13f, P.cyan, true); root.addView(selectedText, margin(0, 8))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(list)
        val import = button("＋  Import WireGuard .conf files", P.raised).apply { textSize = 15f; minHeight = dp(54) }.apply { setTextColor(P.cyan); setOnClickListener { pickConfigs() } }
        root.addView(import, margin(8, 10))
        root.addView(label("CONFIG PRIVACY", 12f, P.cyan, true), margin(10, 4))
        root.addView(label("Profiles stay in memory for this app session and are not uploaded. Import your .conf files again after restarting.", 13f, P.muted), margin(0, 14))
        root.addView(label("DARK NAVY  ·  NEON CYAN  ·  WIREGUARD", 9f, P.muted).apply { gravity = Gravity.CENTER; letterSpacing = 0.035f }, margin(4, 0))
        renderProfiles()
    }

    private fun pickConfigs() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, IMPORT_CONFIG)
    }

    @Deprecated("Uses Activity results for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_PERMISSION) {
            if (resultCode == RESULT_OK) connectVpn() else showStatus("VPN PERMISSION DENIED", "Allow Android's VPN prompt to continue.")
            return
        }
        if (requestCode != IMPORT_CONFIG || resultCode != RESULT_OK || data == null) return
        val uris = mutableListOf<android.net.Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri) }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        var imported = 0
        for (uri in uris) {
            val raw = runCatching {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input)
                    BufferedReader(InputStreamReader(input)).use { it.readText() }
                }
            }.getOrNull() ?: continue
            if (!raw.contains("[Interface]") || !raw.contains("[Peer]") || !Regex("(?m)^\\s*PrivateKey\\s*=").containsMatchIn(raw)) continue
            val endpoint = Regex("(?m)^\\s*Endpoint\\s*=\\s*(.+?)\\s*$").find(raw)?.groupValues?.get(1) ?: continue
            if (profiles.any { it.endpoint == endpoint }) continue
            val mark = Regex("(?m)^\\s*#\\s*([A-Z]{2})-FREE#([0-9]+)").find(raw)
            val code = mark?.groupValues?.get(1) ?: ""
            val number = mark?.groupValues?.get(2) ?: ""
            val country = when (code) { "SG" -> "Singapore"; "RO" -> "Romania"; "NL" -> "Netherlands"; "US" -> "United States"; "CA" -> "Canada"; else -> "Imported server" }
            val flag = when (code) { "SG" -> "🇸🇬"; "RO" -> "🇷🇴"; "NL" -> "🇳🇱"; "US" -> "🇺🇸"; "CA" -> "🇨🇦"; else -> "🌐" }
            profiles.add(Profile(endpoint, flag, country + if (number.isNotEmpty()) " #" + number else "", raw.trim()))
            imported++
        }
        if (profiles.isNotEmpty() && selected >= profiles.size) selected = 0
        renderProfiles()
        Toast.makeText(this, if (imported > 0) "Imported " + imported + " profile(s)" else "No new valid WireGuard configs found", Toast.LENGTH_LONG).show()
    }

    private fun connectVpn() {
        val profile = profiles.getOrNull(selected) ?: run { showStatus("CHOOSE A SERVER", "Import a WireGuard config first."); return }
        showStatus("CONNECTING…", profile.name); connect.isEnabled = false
        Thread {
            try {
                engine.connect("nuvex", profile.config) { state ->
                    runOnUiThread {
                        if (state == Tunnel.State.UP) { connected = true; showStatus("CONNECTED", "Tunnel is up · " + profile.name); connect.text = "Disconnect"; connect.isEnabled = true }
                        else if (state == Tunnel.State.DOWN && connected) { connected = false; showStatus("DISCONNECTED", "VPN tunnel stopped."); connect.text = "Connect"; connect.isEnabled = true }
                    }
                }
                runOnUiThread {
                    connected = engine.currentState() == Tunnel.State.UP
                    showStatus(if (connected) "CONNECTED" else "NOT CONNECTED", if (connected) "Tunnel is up · " + profile.name else "Tunnel did not report an active state.")
                    connect.text = if (connected) "Disconnect" else "Connect"; connect.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread { connected = false; showStatus("CONNECTION FAILED", e.message?.take(100) ?: "Check the config and server."); connect.text = "Try again"; connect.isEnabled = true }
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
                connect.text = "Connect"; connect.isEnabled = true
            }
        }.start()
    }

    private fun showStatus(title: String, message: String) {
        status.text = title; detail.text = message
        status.setTextColor(if (title == "CONNECTED") P.green else if (title.contains("FAILED") || title.contains("ERROR") || title.contains("DENIED")) P.red else P.text)
    }

    private fun renderProfiles() {
        if (!::list.isInitialized) return
        list.removeAllViews(); count.text = profiles.size.toString() + " IMPORTED"
        if (profiles.isEmpty()) {
            list.addView(label("No configs imported yet", 16f, P.text, true), margin(12, 4))
            list.addView(label("Select your five WireGuard .conf files with the button below.", 13f, P.muted), margin(0, 8))
            selectedText.text = "Import configs to select a location"; return
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
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(label(profile.name, 15f, P.text, true)); addView(label(profile.endpoint, 12f, P.muted), margin(3, 0)) }
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
    companion object { private const val IMPORT_CONFIG = 4101; private const val VPN_PERMISSION = 4102 }
}
