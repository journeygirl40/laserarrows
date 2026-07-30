package com.laserarrows.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Web Audio API の oscillator + gain(exponentialRampToValueAtTime)による
 * トーン生成をPCMサンプルとして直接レンダリングする(pure, Android非依存)。
 *
 * 単一オシレーターに加えて、デチューンしたユニゾンボイスと倍音を加算合成できる。
 * これにより「ピッ」という単音ではなく、コーラス感や煌びやかさのある音を作れる。
 */
object PcmToneGenerator {
    fun renderTone(
        freq: Double,
        toFreq: Double?,
        durationSec: Double,
        waveform: Waveform,
        volume: Double,
        sampleRate: Int = 44100,
        /** 立ち上がり時間(秒)。0なら従来通り開始直後がピーク音量のプラック音。 */
        attackSec: Double = 0.0,
        /** ユニゾンボイス数。2以上でデチューンした複数波形を重ねてコーラス感を出す。 */
        unisonVoices: Int = 1,
        /** ユニゾンボイス間のデチューン幅(セント)。 */
        detuneCents: Double = 10.0,
        /** 追加する倍音: (倍音次数, 基音に対する相対音量)。 */
        harmonics: List<Pair<Int, Double>> = emptyList(),
        /** NOISE波形に掛ける1次ローパスの平滑化係数(0=素通し、1に近いほど滑らかな帯域ノイズになる)。 */
        noiseSmoothing: Double = 0.0,
    ): ShortArray {
        val sampleCount = (durationSec * sampleRate).toInt().coerceAtLeast(1)
        val samples = ShortArray(sampleCount)
        val targetFreq = toFreq?.coerceAtLeast(1.0)
        val rnd = Random(System.nanoTime())
        val voices = unisonVoices.coerceAtLeast(1)
        val voicePhases = DoubleArray(voices)
        val harmonicPhases = Array(voices) { DoubleArray(harmonics.size) }
        var noiseState = 0.0

        fun centsToRatio(cents: Double) = 2.0.pow(cents / 1200.0)
        fun waveSample(phase: Double, cyclePos: Double): Double = when (waveform) {
            Waveform.SINE -> sin(phase)
            Waveform.SAWTOOTH -> 2.0 * cyclePos - 1.0
            Waveform.TRIANGLE -> 2.0 * abs(2.0 * cyclePos - 1.0) - 1.0
            Waveform.NOISE -> rnd.nextDouble(-1.0, 1.0)
        }

        val voiceCount = voices.toDouble()
        val harmonicWeight = 1.0 + harmonics.sumOf { it.second }

        for (i in 0 until sampleCount) {
            val t = i / sampleRate.toDouble()
            val progress = (t / durationSec).coerceIn(0.0, 1.0)
            val instantFreq = if (targetFreq != null) {
                freq * (targetFreq / freq).pow(progress)
            } else {
                freq
            }

            var mix = 0.0
            for (v in 0 until voices) {
                val detune = if (voices == 1) 1.0 else centsToRatio((v - (voices - 1) / 2.0) * detuneCents)
                val vFreq = instantFreq * detune
                voicePhases[v] += 2 * PI * vFreq / sampleRate
                val cyclePos = (voicePhases[v] / (2 * PI)).mod(1.0)
                mix += waveSample(voicePhases[v], cyclePos)

                harmonics.forEachIndexed { hi, (order, amp) ->
                    val hPhaseArr = harmonicPhases[v]
                    hPhaseArr[hi] += 2 * PI * vFreq * order / sampleRate
                    val hCycle = (hPhaseArr[hi] / (2 * PI)).mod(1.0)
                    mix += waveSample(hPhaseArr[hi], hCycle) * amp
                }
            }
            mix /= (voiceCount * harmonicWeight)

            if (waveform == Waveform.NOISE && noiseSmoothing > 0.0) {
                noiseState += (mix - noiseState) * (1.0 - noiseSmoothing)
                mix = noiseState
            }

            val attackProgress = if (attackSec > 0.0) (t / attackSec).coerceIn(0.0, 1.0) else 1.0
            val decayEnvelope = volume * (0.001 / volume).pow(progress)
            val envelope = decayEnvelope * attackProgress
            val sample = (mix * envelope).coerceIn(-1.0, 1.0)
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }
}
