package com.laserarrows.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.pow
import kotlin.random.Random

/**
 * 効果音は全て起動時に一度だけPCM波形を生成してWAVに書き出し、SoundPoolへ
 * プリロードしておく。再生の都度AudioTrackを生成していた旧実装は、その生成
 * コストが発射・着弾の瞬間に数msのラグとして聞こえてしまっていたため、
 * 再生時は軽量なsoundPool.play()呼び出しのみで済むようにしている。
 */
class SfxEngine(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(14)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = mutableMapOf<String, Int>()

    /** タップ音のラウンドロビン位置。毎回同じ波形にならないよう順番に切り替える。 */
    private var tapVariant = 0

    init {
        scope.launch(Dispatchers.IO) {
            repeat(SignatureSfx.TAP_VARIANTS) { v -> preloadStereo("tap_$v", SignatureSfx.renderTap(v)) }
            preloadStereo("hit_touch", SignatureSfx.renderHitTouch())
            preloadStereo("heart_arrival", SignatureSfx.renderHeartArrival())
            // ミス音: 濁った倍音を足して虚しさを強調。
            preload("miss_thud", 180.0, 90.0, 0.15, Waveform.SAWTOOTH, attackSec = 0.01, harmonics = listOf(2 to 0.22))
            preload("miss_noise", 700.0, 100.0, 0.12, Waveform.NOISE, noiseSmoothing = 0.55)
            // 衝突音: デチューンを強めにして不協和なうねりを出す。
            preload("clash_ring", 1800.0, 700.0, 0.09, Waveform.TRIANGLE, unisonVoices = 2, detuneCents = 18.0)
            preload("clash_buzz", 220.0, 140.0, 0.1, Waveform.SAWTOOTH, harmonics = listOf(2 to 0.3, 3 to 0.18))
            preload("clash_thud", 140.0, 70.0, 0.13, Waveform.SINE, attackSec = 0.008)
            preload("light_placed", 650.0, 950.0, 0.12, Waveform.SINE, attackSec = 0.01, harmonics = listOf(2 to 0.2))
            preload("clear_blip", 523.0, null, 0.12, Waveform.TRIANGLE, unisonVoices = 2, detuneCents = 5.0, harmonics = listOf(2 to 0.18))
            preload("gameover_blip", 380.0, null, 0.22, Waveform.TRIANGLE, harmonics = listOf(2 to 0.15))
            // ハイスコア更新: 煌びやかなベル状の倍音を持つ専用音。
            preload("sparkle_blip", 880.0, null, 0.16, Waveform.TRIANGLE, attackSec = 0.003, unisonVoices = 2, detuneCents = 6.0, harmonics = listOf(2 to 0.35, 3 to 0.18, 4 to 0.08))
        }
    }

    private fun preload(
        name: String,
        freq: Double,
        toFreq: Double?,
        durationSec: Double,
        waveform: Waveform,
        attackSec: Double = 0.0,
        unisonVoices: Int = 1,
        detuneCents: Double = 10.0,
        harmonics: List<Pair<Int, Double>> = emptyList(),
        noiseSmoothing: Double = 0.0,
    ) {
        val pcm = PcmToneGenerator.renderTone(
            freq, toFreq, durationSec, waveform, volume = 1.0,
            attackSec = attackSec, unisonVoices = unisonVoices, detuneCents = detuneCents,
            harmonics = harmonics, noiseSmoothing = noiseSmoothing,
        )
        val wav = WavEncoder.encode(pcm)
        val file = File(appContext.cacheDir, "sfx_$name.wav")
        file.writeBytes(wav)
        soundIds[name] = soundPool.load(file.path, 1)
    }

    private fun preloadStereo(name: String, pcm: ShortArray) {
        val file = File(appContext.cacheDir, "sfx_$name.wav")
        file.writeBytes(WavEncoder.encode(pcm, SignatureSfx.SAMPLE_RATE, channels = 2))
        soundIds[name] = soundPool.load(file.path, 1)
    }

    private fun play(name: String, volume: Double, rate: Double = 1.0, delayMs: Long = 0) {
        scope.launch {
            if (delayMs > 0) delay(delayMs)
            val id = soundIds[name] ?: return@launch
            val v = volume.toFloat().coerceIn(0f, 1f)
            soundPool.play(id, v, v, 1, 0, rate.toFloat().coerceIn(0.5f, 2.0f))
        }
    }

    /** 矢印をタップした瞬間の木琴風「ポコッ」。3種の波形を順番に使い、音程も±1.5%揺らして機械的な連打感を消す。 */
    fun playFire() {
        val v = tapVariant
        tapVariant = (tapVariant + 1) % SignatureSfx.TAP_VARIANTS
        play("tap_$v", volume = TAP_VOLUME, rate = Random.nextDouble(0.985, 1.015))
    }

    /** ビームが的に届いた瞬間の小さなガラスの「ティン」(直後の到達音の前振り)。 */
    fun playHitConfirm() {
        play("hit_touch", volume = HIT_TOUCH_VOLUME)
    }

    /** 相手の子(ハート)に出会えた瞬間のベルの和音。コンボが続くほどペンタトニックで一段ずつ明るく上がる。 */
    fun playConnect(combo: Int, delayMs: Long = 0) {
        val step = COMBO_STEPS[(combo - 1).coerceIn(0, COMBO_STEPS.lastIndex)]
        play("heart_arrival", volume = HEART_VOLUME, rate = 2.0.pow(step / 12.0), delayMs = delayMs)
    }

    /** クリア画面のパレードで、出会えた組が1組ずつ現れるたびの木琴の音。組ごとにペンタトニックで駆け上がる。 */
    fun playParadePop(index: Int) {
        val step = PARADE_STEPS[index % PARADE_STEPS.size]
        play("tap_${index % SignatureSfx.TAP_VARIANTS}", volume = PARADE_VOLUME, rate = 2.0.pow(step / 12.0))
    }

    /** ★3でクリアした時、パレードの全員が並び終えた瞬間に鳴らす締めのベル。 */
    fun playParadeFinale() {
        play("heart_arrival", volume = PARADE_FINALE_VOLUME, rate = 2.0.pow(7 / 12.0))
    }

    /** タイトルの森が一段にぎやかになった瞬間: 駆け上がるきらめきの後に、温かいベルの和音で締める。 */
    fun playForestLevelUp() {
        playHighScore()
        play("heart_arrival", volume = 0.36, delayMs = 320)
    }

    fun playMiss() {
        play("miss_thud", volume = 0.14)
        play("miss_noise", volume = 0.06)
    }

    /** 矢印同士がぶつかって消えたときの、澄んだ着弾音とは違う濁った衝突音(狙い通りではない感じを出す)。 */
    fun playShooterClash() {
        play("clash_ring", volume = 0.1)
        play("clash_buzz", volume = 0.09, delayMs = 15)
        play("clash_thud", volume = 0.13, delayMs = 30)
    }

    fun playLightPlaced() {
        play("light_placed", volume = 0.12)
    }

    fun playLevelClear() {
        listOf(523.0, 659.0, 784.0, 1046.0).forEachIndexed { i, f ->
            play("clear_blip", volume = 0.13, rate = f / 523.0, delayMs = (i * 80).toLong())
        }
    }

    fun playGameOver() {
        listOf(380.0, 320.0, 260.0, 190.0).forEachIndexed { i, f ->
            play("gameover_blip", volume = 0.12, rate = f / 380.0, delayMs = (i * 140).toLong())
        }
    }

    /** 自己ベストスコアを更新した瞬間の、上昇する煌びやかなアルペジオ。 */
    fun playHighScore() {
        listOf(880.0, 1046.0, 1318.0, 1568.0, 2093.0).forEachIndexed { i, f ->
            play("sparkle_blip", volume = 0.14, rate = f / 880.0, delayMs = (i * 55).toLong())
        }
    }

    companion object {
        private const val TAP_VOLUME = 0.32
        private const val HIT_TOUCH_VOLUME = 0.14
        private const val HEART_VOLUME = 0.42
        /** コンボ1,2,3,4以上での移調幅(半音)。ド→レ→ミ→ソ。高くなりすぎて耳に刺さらないよう5度で止める。 */
        private val COMBO_STEPS = intArrayOf(0, 2, 4, 7)
        private const val PARADE_VOLUME = 0.18
        private const val PARADE_FINALE_VOLUME = 0.3
        /** パレードの音階(半音)。ド・レ・ミ・ソ・ラ・ドを繰り返し上る。 */
        private val PARADE_STEPS = intArrayOf(0, 2, 4, 7, 9, 12)
    }
}
