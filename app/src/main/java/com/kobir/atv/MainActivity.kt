package com.kobir.atv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var player: ExoPlayer
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private var channels = mutableListOf<Channel>()
    private var currentIndex = -1
    private var digits = ""
    private val prefs by lazy { getSharedPreferences("atv", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        player = ExoPlayer.Builder(this).build()
        buildUi()
        prefs.getString("playlist_url", null)?.takeIf { it.isNotBlank() }?.let { loadPlaylist(it) }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 11, 20)); setPadding(24, 18, 24, 18)
        }
        root.addView(TextView(this).apply {
            text = "ATV  /  SMART IPTV"; textSize = 24f
            setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
        })
        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val url = EditText(this).apply {
            hint = "Paste M3U / M3U8 playlist URL"; setHintTextColor(0xFF9AA4B2.toInt())
            setTextColor(Color.WHITE); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        left.addView(url, LinearLayout.LayoutParams(-1, 54))
        left.addView(Button(this).apply {
            text = "LOAD PLAYLIST"; setOnClickListener { loadPlaylist(url.text.toString().trim()) }
        })
        status = TextView(this).apply {
            text = "Add your own playlist URL to begin"; textSize = 13f
            setTextColor(0xFF9AA4B2.toInt()); setPadding(4, 8, 4, 12)
        }
        left.addView(status)
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        left.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        body.addView(left, LinearLayout.LayoutParams(0, -1, 0.34f))
        val playerView = PlayerView(this).apply { player = this@MainActivity.player; useController = true }
        body.addView(playerView, LinearLayout.LayoutParams(0, -1, 0.66f))
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        url.requestFocus()
    }

    private fun loadPlaylist(playlistUrl: String) {
        if (playlistUrl.isBlank()) { toast("Enter a playlist URL"); return }
        status.text = "Loading playlist…"
        prefs.edit().putString("playlist_url", playlistUrl).apply()
        thread {
            try {
                val conn = URL(playlistUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000; conn.readTimeout = 20000
                conn.setRequestProperty("User-Agent", "ATV-Smart-IPTV/1.0")
                val content = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val parsed = M3uParser.parse(content)
                runOnUiThread {
                    channels.clear(); channels.addAll(parsed); renderChannels()
                    status.text = if (channels.isEmpty()) "No channels found. Check playlist." else "${channels.size} channels loaded"
                    if (channels.isNotEmpty()) playChannel(0)
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Playlist error: ${e.localizedMessage ?: "Network error"}" }
            }
        }
    }

    private fun renderChannels() {
        list.removeAllViews()
        channels.forEachIndexed { index, channel ->
            val item = TextView(this).apply {
                text = "%03d  %s".format(channel.number, channel.name)
                textSize = 15f; setTextColor(Color.WHITE); setPadding(14, 16, 14, 16)
                isFocusable = true; isClickable = true
                setBackgroundColor(if (index == currentIndex) 0xFF25345C.toInt() else Color.TRANSPARENT)
                setOnClickListener { playChannel(index) }
                setOnFocusChangeListener { v, focused ->
                    v.setBackgroundColor(if (focused) 0xFF3448C5.toInt() else if (index == currentIndex) 0xFF25345C.toInt() else Color.TRANSPARENT)
                }
            }
            list.addView(item, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun playChannel(index: Int) {
        if (index !in channels.indices) return
        currentIndex = index
        val c = channels[index]
        status.text = "%03d  %s".format(c.number, c.name)
        player.setMediaItem(MediaItem.fromUri(c.url)); player.prepare(); player.playWhenReady = true
        renderChannels()
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { applyPreferredAudio() }
        })
    }

    private fun applyPreferredAudio() {
        if (!prefs.getBoolean("auto_audio", true)) return
        val selected = listOf("bn", "hi", "en").firstOrNull { lang ->
            player.currentTracks.groups.any { group ->
                group.type == C.TRACK_TYPE_AUDIO && (0 until group.length).any { i ->
                    val l = group.getTrackFormat(i).language?.lowercase(Locale.ROOT)
                    l == lang || l?.startsWith("$lang-") == true
                }
            }
        }
        if (selected != null) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setPreferredAudioLanguage(selected).build()
        }
    }

    private fun showAudioTracks() {
        val options = mutableListOf<String>()
        val langs = mutableListOf<String>()
        player.currentTracks.groups.forEach { group ->
            if (group.type == C.TRACK_TYPE_AUDIO) for (i in 0 until group.length) {
                val f = group.getTrackFormat(i)
                options += "${f.language ?: "Unknown"} • ${f.label ?: "Audio ${i + 1}"}"
                langs += f.language ?: ""
            }
        }
        if (options.isEmpty()) { toast("No selectable audio tracks reported"); return }
        AlertDialog.Builder(this).setTitle("Audio tracks").setItems(options.toTypedArray()) { _, which ->
            val lang = langs[which]
            if (lang.isNotBlank()) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setPreferredAudioLanguage(lang).build()
                prefs.edit().putString("manual_audio", lang).apply()
            }
        }.setNegativeButton("Close", null).show()
    }

    private fun zap(delta: Int) {
        if (channels.isNotEmpty()) playChannel((currentIndex + delta).coerceIn(0, channels.lastIndex))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) when (event.keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> { zap(1); return true }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> { zap(-1); return true }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { if (player.isPlaying) player.pause() else player.play(); return true }
            KeyEvent.KEYCODE_MEDIA_PLAY -> { player.play(); return true }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> { player.pause(); return true }
            KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK, KeyEvent.KEYCODE_MENU -> { showAudioTracks(); return true }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                digits += (event.keyCode - KeyEvent.KEYCODE_0).toString()
                status.text = "Tuning: $digits"; return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> if (digits.isNotEmpty()) {
                val idx = channels.indexOfFirst { it.number == digits.toIntOrNull() }
                digits = ""
                if (idx >= 0) playChannel(idx) else toast("Channel number not found")
                return true
            }
            KeyEvent.KEYCODE_DEL -> if (digits.isNotEmpty()) {
                digits = digits.dropLast(1); status.text = "Tuning: $digits"; return true
            }
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> if (digits.isNotEmpty()) {
                digits = ""; status.text = "Channel entry cancelled"; return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    override fun onStop() { super.onStop(); player.pause() }
    override fun onDestroy() { player.release(); super.onDestroy() }
}
