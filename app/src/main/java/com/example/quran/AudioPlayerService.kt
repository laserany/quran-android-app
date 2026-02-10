package com.example.quran

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.os.IBinder
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class AudioPlayerService : Service() {

    // ... (Your existing variables remain the same) ...
    private var player: SimpleExoPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val PREF_KEY_LAST_PLAYED_POSITION = "last_played_position"
    private val PREF_KEY_LAST_PLAYED_MEDIA_INDEX = "last_played_media_index"
    private val SOUAR_NUMBER = 114
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "QuranAudioChannel"

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("QuranPrefs", Context.MODE_PRIVATE)
        createNotificationChannel()

        player = SimpleExoPlayer.Builder(this).build()

        // Add listener to save while playing (using async apply is fine here)
        player?.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // If playWhenReady is false, the user has PAUSED the player
                if (!playWhenReady) {
                    saveCurrentPosition(async = true)
                }
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                saveCurrentPosition(async = true)
            }
        })

        // ... (Rest of your notification setup) ...
        playerNotificationManager = PlayerNotificationManager.Builder(
            this, NOTIFICATION_ID, CHANNEL_ID
        ).setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                if (ongoing) startForeground(notificationId, notification)
            }
            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                stopSelf()
            }
        }).build()

        playerNotificationManager?.setPlayer(player)
        loadMedia()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // CRITICAL FIX: Use commit() (synchronous) here
        saveCurrentPosition(async = false)
        stopSelf() // Optional: Ensure service stops if that is your intent
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // CRITICAL FIX: Use commit() here as well just in case
        saveCurrentPosition(async = false)

        player?.release()
        playerNotificationManager?.setPlayer(null)
        super.onDestroy()
    }

    // Unified save method
    private fun saveCurrentPosition(async: Boolean) {
        player?.let { exoPlayer ->
            val editor = sharedPreferences.edit()
            editor.putInt(PREF_KEY_LAST_PLAYED_MEDIA_INDEX, exoPlayer.currentWindowIndex)
            editor.putLong(PREF_KEY_LAST_PLAYED_POSITION, exoPlayer.currentPosition)

            if (async) {
                editor.apply()  // Faster, use during normal playback
            } else {
                editor.commit() // Slower but GUARANTEES write before process death
            }
        }
    }

    // ... (Your loadMedia, createNotificationChannel, and onBind remain the same) ...
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Quran Playback"
            val descriptionText = "Controls for Quran Audio"
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun loadMedia() {
        // ... (Keep your existing loadMedia implementation) ...
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Quran"))

        for (i in 1..SOUAR_NUMBER) {
            val suraNumber = String.format("%03d", i)
            val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Quick Share/${suraNumber}.mp3"
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(filePath))
            player?.addMediaSource(mediaSource)
        }

        player?.prepare()

        val lastPlayedPosition = sharedPreferences.getLong(PREF_KEY_LAST_PLAYED_POSITION, C.TIME_UNSET)
        val lastPlayedMediaIndex = sharedPreferences.getInt(PREF_KEY_LAST_PLAYED_MEDIA_INDEX, 0)

        if (lastPlayedPosition != C.TIME_UNSET) {
            player?.seekTo(lastPlayedMediaIndex, lastPlayedPosition)
        }
        player?.repeatMode = Player.REPEAT_MODE_ALL
        player?.playWhenReady = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}