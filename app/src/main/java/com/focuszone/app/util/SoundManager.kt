package com.focuszone.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var soundComplete = 0
    private var soundVictory  = 0
    private var soundLevelUp  = 0
    private var soundPenalty  = 0
    private var isLoaded      = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) isLoaded = true
        }

        loadSounds()
    }

    private fun loadSounds() {
        // Sounds are optional — if the files are not in res/raw/ yet, we skip silently
        soundComplete = loadRawSound("sound_complete")
        soundVictory  = loadRawSound("sound_victory")
        soundLevelUp  = loadRawSound("sound_levelup")
        soundPenalty  = loadRawSound("sound_penalty")
    }

    private fun loadRawSound(name: String): Int {
        return try {
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) soundPool?.load(context, resId, 1) ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    fun playSessionComplete() = play(soundComplete)
    fun playVictory()         = play(soundVictory)
    fun playLevelUp()         = play(soundLevelUp)
    fun playPenalty()         = play(soundPenalty, volume = 0.8f)

    private fun play(soundId: Int, volume: Float = 1f) {
        if (isLoaded && soundId != 0) {
            soundPool?.play(soundId, volume, volume, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded  = false
    }
}