package com.laserarrows.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * タップ音と、相手の子(ハート)に届いた瞬間の音を、打楽器の物理モデル風の加算合成+ステレオ残響で
 * 描き出す(pure, Android非依存)。出力は16bitステレオのインターリーブPCM。
 *
 * 何百回も鳴る音なので「何度聞いても疲れない」ことを最優先にしている:
 * - 耳に刺さる3〜5kHz帯や矩形波的な倍音を避け、基音中心の丸い音色にする。
 * - 部分音ごとに減衰時間を変え(高い部分音ほど早く消える)、本物の楽器を録ったような自然な鳴り終わりにする。
 * - 立ち上がりは数msのレイズドコサインでクリックノイズを消し、打鍵感は低域の胴鳴りと柔らかいノイズで出す。
 * - 同じ音の機械的な繰り返しに聞こえないよう、タップ音は僅かに違う複数バリエーションを用意する。
 */
object SignatureSfx {
    const val SAMPLE_RATE = 44100
    const val TAP_VARIANTS = 3

    private class Partial(val ratio: Double, val amp: Double, val decaySec: Double)

    /** 木琴をフェルトのマレットでそっと叩いたような「ポコッ」。variantごとに音程と倍音の混ざり方を少しだけ変える。 */
    fun renderTap(variant: Int): ShortArray {
        val buf = Stereo(sec(0.42))
        val v = variant.coerceIn(0, TAP_VARIANTS - 1)
        val partials = listOf(
            Partial(1.0, 1.0, 0.085),
            Partial(0.5, 0.34, 0.05), // 胴鳴り(低域の厚み)
            Partial(3.98, 0.09 + 0.02 * v, 0.024), // 木琴らしい4倍付近の部分音
            Partial(9.2, 0.02, 0.008), // マレットが当たる瞬間の硬さ
        )
        buf.addStruck(
            659.25 * (1.0 + (v - 1) * 0.004), partials, startSec = 0.0, attackSec = 0.0015, gain = 1.0,
            pan = (v - 1) * 0.06, pitchDrop = 0.03, pitchDropSec = 0.012,
        )
        buf.addNoiseBurst(Random(1000 + v), durSec = 0.006, gain = 0.08, smoothing = 0.62)
        buf.reverb(room = 0.5, damp = 0.5, wet = 0.11, width = 0.8)
        return buf.toPcm()
    }

    /** ビームが相手に届いた瞬間の、ごく小さなガラスの「ティン」(直後の到達音の前振り)。 */
    fun renderHitTouch(): ShortArray {
        val buf = Stereo(sec(0.4))
        buf.addStruck(
            1567.98, listOf(Partial(1.0, 1.0, 0.07), Partial(2.0, 0.12, 0.03)),
            startSec = 0.0, attackSec = 0.001, gain = 1.0, pan = 0.1,
        )
        buf.reverb(room = 0.55, damp = 0.45, wet = 0.16, width = 0.9)
        return buf.toPcm()
    }

    /**
     * 相手の子に出会えた瞬間の「きらん♪」。チェレスタ風のベル3音(ソ・ド・ミ)を軽くかき鳴らし、
     * 下に柔らかいパッド(ド)を敷いて温かみを足し、広めの残響で余韻を残す。
     */
    fun renderHeartArrival(): ShortArray {
        val buf = Stereo(sec(1.9))
        val bell = listOf(
            Partial(1.0, 1.0, 0.55),
            Partial(2.0, 0.22, 0.28),
            Partial(3.0, 0.07, 0.14),
            Partial(5.43, 0.035, 0.05), // 鍵盤を叩いた瞬間だけのきらめき
        )
        listOf(783.99 to -0.32, 1046.5 to 0.04, 1318.51 to 0.32).forEachIndexed { i, (f, pan) ->
            buf.addStruck(f, bell, startSec = i * 0.032, attackSec = 0.002, gain = 0.62 - i * 0.09, pan = pan)
        }
        buf.addPad(261.63, startSec = 0.0, attackSec = 0.035, decaySec = 0.42, gain = 0.3)
        buf.addPad(523.25, startSec = 0.01, attackSec = 0.03, decaySec = 0.35, gain = 0.2)
        buf.addStruck(2093.0, listOf(Partial(1.0, 1.0, 0.18)), startSec = 0.075, attackSec = 0.003, gain = 0.07, pan = 0.45)
        buf.reverb(room = 0.82, damp = 0.45, wet = 0.27, width = 1.0)
        return buf.toPcm()
    }

    private fun sec(s: Double) = (s * SAMPLE_RATE).toInt()

    private class Stereo(val length: Int) {
        val l = DoubleArray(length)
        val r = DoubleArray(length)

        /** 等パワーのパン(-1=左, 0=中央, 1=右)。 */
        private fun panGains(pan: Double): Pair<Double, Double> {
            val a = (pan.coerceIn(-1.0, 1.0) + 1.0) * PI / 4
            return cos(a) to sin(a)
        }

        /** 叩いた瞬間に僅かに音程が上ずってすぐ落ち着く、打楽器の減衰音。 */
        fun addStruck(
            freq: Double,
            partials: List<Partial>,
            startSec: Double,
            attackSec: Double,
            gain: Double,
            pan: Double,
            pitchDrop: Double = 0.0,
            pitchDropSec: Double = 0.01,
        ) {
            val (gl, gr) = panGains(pan)
            val start = sec(startSec)
            val audible = partials.filter { freq * it.ratio < SAMPLE_RATE * 0.4 }
            val phases = DoubleArray(audible.size)
            for (i in start until length) {
                val t = (i - start) / SAMPLE_RATE.toDouble()
                val bend = 1.0 + pitchDrop * exp(-t / pitchDropSec)
                val attack = if (t < attackSec) 0.5 - 0.5 * cos(PI * t / attackSec) else 1.0
                var s = 0.0
                audible.forEachIndexed { p, partial ->
                    phases[p] += 2 * PI * freq * partial.ratio * bend / SAMPLE_RATE
                    s += sin(phases[p]) * partial.amp * exp(-t / partial.decaySec)
                }
                s *= gain * attack
                l[i] += s * gl
                r[i] += s * gr
            }
        }

        /** ゆっくり立ち上がって消えるサイン波の下地(左右に僅かにずらしてふくらみを出す)。 */
        fun addPad(freq: Double, startSec: Double, attackSec: Double, decaySec: Double, gain: Double) {
            val start = sec(startSec)
            var pl = 0.0
            var pr = 0.0
            for (i in start until length) {
                val t = (i - start) / SAMPLE_RATE.toDouble()
                val attack = if (t < attackSec) 0.5 - 0.5 * cos(PI * t / attackSec) else 1.0
                val env = gain * attack * exp(-t / decaySec)
                pl += 2 * PI * freq * 0.9985 / SAMPLE_RATE
                pr += 2 * PI * freq * 1.0015 / SAMPLE_RATE
                l[i] += sin(pl) * env
                r[i] += sin(pr) * env
            }
        }

        /** マレットが当たる「コツ」を、ローパスを掛けた短いノイズで足す。 */
        fun addNoiseBurst(rnd: Random, durSec: Double, gain: Double, smoothing: Double) {
            val n = sec(durSec).coerceAtMost(length)
            var state = 0.0
            for (i in 0 until n) {
                state += (rnd.nextDouble(-1.0, 1.0) - state) * (1.0 - smoothing)
                val env = gain * (1.0 - i / n.toDouble()).let { it * it }
                l[i] += state * env
                r[i] += state * env
            }
        }

        /** Freeverb(Schroeder-Moorer型)の残響。左右でディレイ長をずらして広がりを出す。 */
        fun reverb(room: Double, damp: Double, wet: Double, width: Double) {
            val feedback = 0.7 + room * 0.28
            val damp1 = damp * 0.4
            val wetL = channelReverb(feedback, damp1, 0)
            val wetR = channelReverb(feedback, damp1, STEREO_SPREAD)
            val wet1 = wet * 3.0 * (width / 2 + 0.5)
            val wet2 = wet * 3.0 * ((1 - width) / 2)
            for (i in 0 until length) {
                val outL = wetL[i] * wet1 + wetR[i] * wet2
                val outR = wetR[i] * wet1 + wetL[i] * wet2
                l[i] += outL
                r[i] += outR
            }
        }

        private fun channelReverb(feedback: Double, damp: Double, spread: Int): DoubleArray {
            val combs = COMB_TUNING.map { DoubleArray(it + spread) }
            val combIdx = IntArray(combs.size)
            val combStore = DoubleArray(combs.size)
            val allpasses = ALLPASS_TUNING.map { DoubleArray(it + spread) }
            val apIdx = IntArray(allpasses.size)
            val out = DoubleArray(length)
            for (i in 0 until length) {
                val input = (l[i] + r[i]) * 0.5 * 0.015
                var acc = 0.0
                combs.forEachIndexed { c, buf ->
                    val y = buf[combIdx[c]]
                    combStore[c] = y * (1 - damp) + combStore[c] * damp
                    buf[combIdx[c]] = input + combStore[c] * feedback
                    combIdx[c] = (combIdx[c] + 1) % buf.size
                    acc += y
                }
                allpasses.forEachIndexed { a, buf ->
                    val y = buf[apIdx[a]]
                    buf[apIdx[a]] = acc + y * 0.5
                    apIdx[a] = (apIdx[a] + 1) % buf.size
                    acc = y - acc
                }
                out[i] = acc
            }
            return out
        }

        /** ピークを揃えて(-1dBFS)16bitインターリーブに変換する。末尾は短くフェードして途切れのプツッを防ぐ。 */
        fun toPcm(): ShortArray {
            val fade = sec(0.03).coerceAtMost(length)
            for (k in 0 until fade) {
                val g = k / fade.toDouble()
                l[length - 1 - k] *= g
                r[length - 1 - k] *= g
            }
            var peak = 1e-9
            for (i in 0 until length) peak = maxOf(peak, abs(l[i]), abs(r[i]))
            val scale = 0.89 / peak
            val pcm = ShortArray(length * 2)
            for (i in 0 until length) {
                pcm[2 * i] = (l[i] * scale * Short.MAX_VALUE).toInt().toShort()
                pcm[2 * i + 1] = (r[i] * scale * Short.MAX_VALUE).toInt().toShort()
            }
            return pcm
        }
    }

    private val COMB_TUNING = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val ALLPASS_TUNING = intArrayOf(556, 441, 341, 225)
    private const val STEREO_SPREAD = 23
}
