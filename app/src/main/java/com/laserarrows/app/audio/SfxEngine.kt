package com.laserarrows.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.laserarrows.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

    /** 発射音(laser.mp3)の再生中ストリームID。着弾音が鳴り始めたら途中でも打ち切る。 */
    private var fireStreamId: Int = 0

    init {
        soundIds["laser"] = soundPool.load(appContext, R.raw.laser, 1)
        soundIds["fireflower"] = soundPool.load(appContext, R.raw.fireflower, 1)
        scope.launch(Dispatchers.IO) {
            preload("hit_tick", 3000.0, 4200.0, 0.035, Waveform.SINE, harmonics = listOf(2 to 0.25))
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

    private fun play(name: String, volume: Double, rate: Double = 1.0, delayMs: Long = 0) {
        scope.launch {
            if (delayMs > 0) delay(delayMs)
            val id = soundIds[name] ?: return@launch
            val v = volume.toFloat().coerceIn(0f, 1f)
            soundPool.play(id, v, v, 1, 0, rate.toFloat().coerceIn(0.5f, 2.0f))
        }
    }

    /** 発射音(laser.mp3)。着弾音(playConnect)が鳴り始めたら途切れさせる。 */
    fun playFire() {
        val id = soundIds["laser"] ?: return
        fireStreamId = soundPool.play(id, FIRE_VOLUME, FIRE_VOLUME, 1, 0, 1f)
    }

    /** ビームが的に届いた瞬間の短く軽いタッチ音(直後の着弾音の前振り)。 */
    fun playHitConfirm() {
        play("hit_tick", volume = 0.1)
    }

    /** 受光して消える瞬間の音(fireflower.mp3)。発射音が鳴り続けていれば打ち切ってから鳴らし、
     * 全体を3秒として2秒地点から3秒地点にかけてフェードアウトさせる。 */
    fun playConnect(@Suppress("UNUSED_PARAMETER") combo: Int) {
        if (fireStreamId != 0) {
            soundPool.stop(fireStreamId)
            fireStreamId = 0
        }
        val id = soundIds["fireflower"] ?: return
        val streamId = soundPool.play(id, CONNECT_VOLUME, CONNECT_VOLUME, 1, 0, 1f)
        if (streamId == 0) return
        scope.launch {
            delay(FADE_START_MS)
            val steps = 20
            val stepDelayMs = (FADE_DURATION_MS / steps).coerceAtLeast(1L)
            for (step in 1..steps) {
                val v = (CONNECT_VOLUME * (1f - step.toFloat() / steps)).coerceAtLeast(0f)
                soundPool.setVolume(streamId, v, v)
                delay(stepDelayMs)
            }
            soundPool.stop(streamId)
        }
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
        private const val FIRE_VOLUME = 0.4f
        private const val CONNECT_VOLUME = 0.4f
        // fireflower.mp3は全体3秒を想定し、2秒地点から3秒地点(=1秒かけて)フェードアウトする。
        private const val FADE_START_MS = 2000L
        private const val FADE_DURATION_MS = 1000L
    }
}
