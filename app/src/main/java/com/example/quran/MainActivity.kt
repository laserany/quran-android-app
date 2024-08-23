package com.example.quran

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity() {
    private var player: SimpleExoPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_KEY_LAST_PLAYED_POSITION = "last_played_position"
    private val PREF_KEY_LAST_PLAYED_MEDIA_INDEX = "last_played_media_index"
    private val SOUAR_NUMBER = 114

    private fun buildMediaSource(filePath: String): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Quran"))
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(filePath))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getPreferences(MODE_PRIVATE)
        player = SimpleExoPlayer.Builder(this).build()
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 1)
        }
        else {
            for (i in 1..SOUAR_NUMBER) {
                val suraNumber = String.format("%03d", i)
                val filePath = Environment.getExternalStorageDirectory().absolutePath + "/Download/fm_002_20150413_0654/${suraNumber}.mp3"
                val mediaSource = buildMediaSource(filePath)
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

    }

    override fun onStop() {
        super.onStop()
        // Save the last played position when the activity is stopped
        player?.let {
            val lastPlayedPosition = it.currentPosition
            val currentMediaIndex = it.currentWindowIndex
            sharedPreferences.edit().putInt(PREF_KEY_LAST_PLAYED_MEDIA_INDEX, currentMediaIndex).putLong(PREF_KEY_LAST_PLAYED_POSITION, lastPlayedPosition).apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the player when the activity is destroyed to free up resources.
        player?.release()
    }

}