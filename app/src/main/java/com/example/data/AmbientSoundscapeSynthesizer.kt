package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

enum class SoundscapeType(val displayName: String, val icon: String, val description: String) {
    NONE("Off", "🚫", "No ambient soundscape"),
    RAINY_CAFE("Rainy Cafe", "🌧️", "Soothing rain & cozy cafe ambience"),
    CRACKLING_FIRE("Fireplace Hearth", "🔥", "Warm crackling fire and soft embers"),
    COSMIC_DRONE("Cosmic Ambient", "🌌", "Deep space harmonic drone & binaural peace"),
    FOREST_SOLITUDE("Forest Sanctuary", "🌲", "Gentle breeze & serene nature whispers"),
    PIANO_LOFI("Lo-Fi Study", "🎹", "Soft relaxing piano chords & gentle hum"),
    AUTO_DETECT("Auto Mood Match", "✨", "Gemini matches soundscape to current book text")
}

object AmbientSoundscapeSynthesizer {
    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var synthesizerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _currentSoundscape = MutableStateFlow(SoundscapeType.NONE)
    val currentSoundscape: StateFlow<SoundscapeType> = _currentSoundscape.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (e: Exception) {
            Log.e("AmbientSynth", "Error setting volume", e)
        }
    }

    fun startSoundscape(type: SoundscapeType) {
        stopSoundscape()
        if (type == SoundscapeType.NONE || type == SoundscapeType.AUTO_DETECT) {
            _currentSoundscape.value = type
            return
        }

        _currentSoundscape.value = type
        synthesizerJob = scope.launch {
            generateAudioLoop(type)
        }
    }

    fun stopSoundscape() {
        synthesizerJob?.cancel()
        synthesizerJob = null
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AmbientSynth", "Error releasing track", e)
        }
        audioTrack = null
        _currentSoundscape.value = SoundscapeType.NONE
    }

    private fun generateAudioLoop(type: SoundscapeType) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = minBufferSize.coerceAtLeast(8192)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.apply {
            setVolume(_volume.value)
            play()
        }

        val shortBuffer = ShortArray(bufferSize / 2)
        val random = Random()

        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        var filterValL = 0.0
        var filterValR = 0.0

        while (scope.isActive && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
            val vol = _volume.value
            for (i in 0 until shortBuffer.size step 2) {
                var sampleL = 0.0
                var sampleR = 0.0

                when (type) {
                    SoundscapeType.RAINY_CAFE -> {
                        // Pink/Brown noise rain filter with soft rumble
                        val rawNoiseL = (random.nextDouble() * 2.0 - 1.0)
                        val rawNoiseR = (random.nextDouble() * 2.0 - 1.0)
                        filterValL = 0.94 * filterValL + 0.06 * rawNoiseL
                        filterValR = 0.94 * filterValR + 0.06 * rawNoiseR

                        // Gentle cafe low hum
                        phase1 += 2.0 * Math.PI * 110.0 / SAMPLE_RATE
                        val cafeHum = sin(phase1) * 0.05

                        sampleL = filterValL * 0.35 + cafeHum
                        sampleR = filterValR * 0.35 + cafeHum
                    }
                    SoundscapeType.CRACKLING_FIRE -> {
                        // Warm low hum + randomized sharp pops
                        phase1 += 2.0 * Math.PI * 65.0 / SAMPLE_RATE
                        val warmHearth = sin(phase1) * 0.2

                        val crackle = if (random.nextDouble() < 0.003) {
                            (random.nextDouble() * 2.0 - 1.0) * 0.8
                        } else {
                            (random.nextDouble() * 2.0 - 1.0) * 0.05
                        }
                        filterValL = 0.88 * filterValL + 0.12 * crackle
                        sampleL = warmHearth + filterValL
                        sampleR = warmHearth + filterValL * 0.9
                    }
                    SoundscapeType.COSMIC_DRONE -> {
                        // Harmonic binaural drone (108Hz and 112Hz)
                        phase1 += 2.0 * Math.PI * 108.0 / SAMPLE_RATE
                        phase2 += 2.0 * Math.PI * 162.0 / SAMPLE_RATE
                        phase3 += 2.0 * Math.PI * 216.0 / SAMPLE_RATE

                        val drone1 = sin(phase1) * 0.25
                        val drone2 = sin(phase2) * 0.15
                        val drone3 = sin(phase3) * 0.08

                        sampleL = drone1 + drone2
                        sampleR = drone1 + drone3
                    }
                    SoundscapeType.FOREST_SOLITUDE -> {
                        // Wind modulation and high rustle
                        phase1 += 2.0 * Math.PI * 0.2 / SAMPLE_RATE // slow wind modulation
                        val windMod = (sin(phase1) + 1.0) * 0.5

                        val breeze = (random.nextDouble() * 2.0 - 1.0)
                        filterValL = 0.92 * filterValL + 0.08 * breeze
                        sampleL = filterValL * windMod * 0.3
                        sampleR = filterValL * (1.0 - windMod * 0.5) * 0.3
                    }
                    SoundscapeType.PIANO_LOFI -> {
                        // Soft soothing chord tone + warm vinyl hiss
                        phase1 += 2.0 * Math.PI * 220.0 / SAMPLE_RATE // A3
                        phase2 += 2.0 * Math.PI * 277.18 / SAMPLE_RATE // C#4
                        phase3 += 2.0 * Math.PI * 329.63 / SAMPLE_RATE // E4

                        val pianoChord = (sin(phase1) * 0.2 + sin(phase2) * 0.15 + sin(phase3) * 0.1) * 0.4
                        val vinylHiss = (random.nextDouble() * 2.0 - 1.0) * 0.03

                        sampleL = pianoChord + vinylHiss
                        sampleR = pianoChord + vinylHiss
                    }
                    else -> {
                        sampleL = 0.0
                        sampleR = 0.0
                    }
                }

                shortBuffer[i] = (sampleL * 32767 * vol).toInt().coerceIn(-32768, 32767).toShort()
                shortBuffer[i + 1] = (sampleR * 32767 * vol).toInt().coerceIn(-32768, 32767).toShort()
            }

            audioTrack?.write(shortBuffer, 0, shortBuffer.size)
        }
    }
}
