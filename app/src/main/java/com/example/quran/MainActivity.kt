package com.example.quran

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer

class MainActivity : AppCompatActivity() {
    private var player: SimpleExoPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_KEY_LAST_PLAYED_POSITION = "last_played_position"
    private val PREF_KEY_LAST_PLAYED_MEDIA_INDEX = "last_played_media_index"
    private val SOUAR_NUMBER = 114
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getPreferences(MODE_PRIVATE)
        player = SimpleExoPlayer.Builder(this).build()
        for (i in 1..SOUAR_NUMBER) {
            val suraNumber = String.format("%03d", i)
            val mp3Item = MediaItem.fromUri("https://server8.mp3quran.net/afs/${suraNumber}.mp3")
            player?.addMediaItem(mp3Item)
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