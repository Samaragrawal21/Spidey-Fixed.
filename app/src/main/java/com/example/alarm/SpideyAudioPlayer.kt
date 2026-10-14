package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.R

class SpideyAudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "SpideyAudioPlayer"
        private var sharedMediaPlayer: MediaPlayer? = null
        private val lock = Any()

        fun isCurrentlyPlaying(): Boolean = synchronized(lock) {
            try {
                sharedMediaPlayer?.isPlaying == true
            } catch (_: Exception) {
                false
            }
        }

        fun stop() {
            synchronized(lock) {
                try {
                    sharedMediaPlayer?.let { mp ->
                        try {
                            if (mp.isPlaying) {
                                mp.stop()
                            }
                        } catch (_: Exception) {}
                        try {
                            mp.reset()
                        } catch (_: Exception) {}
                        try {
                            mp.release()
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping audio player", e)
                } finally {
                    sharedMediaPlayer = null
                }
            }
        }
    }

    fun playAlertSound() {
        synchronized(lock) {
            try {
                // If already actively playing the alert sound, do NOT stop or restart prematurely
                if (sharedMediaPlayer?.isPlaying == true) {
                    Log.d(TAG, "Alert sound already actively playing, continuing playback")
                    return
                }

                stop()

                val afd = context.resources.openRawResourceFd(R.raw.black_suit_theme)
                if (afd == null) {
                    Log.e(TAG, "Failed to open raw resource R.raw.black_suit_theme")
                    return
                }

                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                mp.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Playback error: what=$what, extra=$extra")
                    false
                }

                mp.setOnCompletionListener { player ->
                    Log.d(TAG, "Playback reached end of track, restarting loop")
                    try {
                        player.seekTo(0)
                        player.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restarting playback loop", e)
                    }
                }

                mp.prepare()
                mp.isLooping = true
                mp.seekTo(0)
                mp.start()

                sharedMediaPlayer = mp
                Log.d(TAG, "Alert sound started successfully. Looping enabled. Duration=${mp.duration}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Audio initialization failed", e)
                stop()
            }
        }
    }

    fun stopAndRelease() {
        stop()
    }
}
