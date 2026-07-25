package com.laserarrows.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Web Audio API の oscillator + gain(exponentialRampToValueAtTime)による
 * トーン生成をPCMサンプルとして直接レンダリングする(pure, Android非依存)。
 */
object PcmToneGenerator {
    fun renderTone(
        freq: Double,
        toFreq: Double?,
        durationSec: Double,
        waveform: Waveform,
        volume: Double,
        sampleRate: Int = 44100,
    ): ShortArray {
        val sampleCount = (durationSec * sampleRate).toInt().coerceAtLeast(1)
        val samples = ShortArray(sampleCount)
        val targetFreq = toFreq?.coerceAtLeast(1.0)
        val rnd = Random(System.nanoTime())
        var phase = 0.0
        for (i in 0 until sampleCount) {
            val t = i / sampleRate.toDouble()
            val progress = (t / durationSec).coerceIn(0.0, 1.0)
            val instantFreq = if (targetFreq != null) {
                freq * (targetFreq / freq).pow(progress)
            } else {
                freq
            }
            phase += 2 * PI * instantFreq / sampleRate
            val cyclePos = (phase / (2 * PI)).mod(1.0)
            val raw = when (waveform) {
                Waveform.SINE -> sin(phase)
                Waveform.SAWTOOTH -> 2.0 * cyclePos - 1.0
                Waveform.TRIANGLE -> 2.0 * abs(2.0 * cyclePos - 1.0) - 1.0
                Waveform.NOISE -> rnd.nextDouble(-1.0, 1.0)
            }
            val envelope = volume * (0.001 / volume).pow(progress)
            val sample = (raw * envelope).coerceIn(-1.0, 1.0)
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }
}
