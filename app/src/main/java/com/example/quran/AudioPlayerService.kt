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
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class AudioPlayerService : Service() {

    private var player: SimpleExoPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var playerNotificationManager: PlayerNotificationManager? = null

    // Keys
    private val PREF_KEY_LAST_PLAYED_POSITION = "last_played_position"
    private val PREF_KEY_LAST_PLAYED_MEDIA_INDEX = "last_played_media_index"
    private val SOUAR_NUMBER = 114
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "QuranAudioChannel"

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("QuranPrefs", Context.MODE_PRIVATE)

        // 1. CREATE THE CHANNEL FIRST (Crucial for Android 13+)
        createNotificationChannel()

        // 2. Initialize Player
        player = SimpleExoPlayer.Builder(this).build()

        // 3. Setup Notification Manager
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        ).setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                // Check for permission on Android 13+ before calling startForeground
                if (ongoing) {
                    startForeground(notificationId, notification)
                }
            }
            // ... rest of your listener code
        }).build()

        playerNotificationManager?.setPlayer(player)
        loadMedia()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
// 1. Save the state immediately
        saveCurrentPosition()

        // 2. Decide behavior:
        // If you want the music to STOP when swiped away:
        // stopSelf()

        // If you want it to KEEP playing (like Spotify):
        // Do nothing, but ensure your notification is in "Foreground" mode
        super.onTaskRemoved(rootIntent)
    }

    private fun setupPlayer() {
        player = SimpleExoPlayer.Builder(this).build()

        player?.addListener(object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                saveCurrentPosition()
            }

            // Save whenever the user pauses or the track changes
            override fun onPlaybackStateChanged(state: Int) {
                saveCurrentPosition()
            }
        })
    }

    private fun saveCurrentPosition() {
        player?.let {
            sharedPreferences.edit()
                .putInt(PREF_KEY_LAST_PLAYED_MEDIA_INDEX, it.currentWindowIndex)
                .putLong(PREF_KEY_LAST_PLAYED_POSITION, it.currentPosition)
                .apply() // .apply() is asynchronous and safer here
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Quran Playback"
            val descriptionText = "Controls for Quran Audio"
            val importance = android.app.NotificationManager.IMPORTANCE_LOW // Use LOW so it doesn't 'beep' every time
            val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun loadMedia() {
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Quran"))

        for (i in 1..SOUAR_NUMBER) {
            val suraNumber = String.format("%03d", i)
            val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Quick Share/${suraNumber}.mp3"
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(filePath))
            player?.addMediaSource(mediaSource)
        }

        player?.prepare()

        // Restore State
        val lastPlayedPosition = sharedPreferences.getLong(PREF_KEY_LAST_PLAYED_POSITION, C.TIME_UNSET)
        val lastPlayedMediaIndex = sharedPreferences.getInt(PREF_KEY_LAST_PLAYED_MEDIA_INDEX, 0)

        if (lastPlayedPosition != C.TIME_UNSET) {
            player?.seekTo(lastPlayedMediaIndex, lastPlayedPosition)
        }

        player?.repeatMode = Player.REPEAT_MODE_ALL
        player?.playWhenReady = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY // Restart service if system kills it
    }

    override fun onDestroy() {
        // Save state before destroying
        player?.let {
            sharedPreferences.edit()
                .putInt(PREF_KEY_LAST_PLAYED_MEDIA_INDEX, it.currentWindowIndex)
                .putLong(PREF_KEY_LAST_PLAYED_POSITION, it.currentPosition)
                .commit()
            it.release()
        }
        playerNotificationManager?.setPlayer(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't need binding for this simple use case
    }
}