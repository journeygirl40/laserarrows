package com.laserarrows.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
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

    init {
        scope.launch(Dispatchers.IO) {
            preload("fire_sweep_high", 5500.0, 1600.0, 0.055, Waveform.SAWTOOTH)
            preload("fire_sweep_mid", 3200.0, 1100.0, 0.04, Waveform.TRIANGLE)
            preload("fire_noise", 7000.0, 2500.0, 0.025, Waveform.NOISE)
            preload("hit_tick", 3000.0, 4200.0, 0.035, Waveform.SINE)
            preload("connect_crack", 4500.0, 900.0, 0.05, Waveform.NOISE)
            preload("connect_shard_low", 2400.0, 1200.0, 0.16, Waveform.TRIANGLE)
            preload("connect_shard_high", 6000.0, 3000.0, 0.16, Waveform.TRIANGLE)
            preload("connect_impact", 85.0, 38.0, 0.11, Waveform.SINE)
            preload("miss_thud", 180.0, 90.0, 0.15, Waveform.SAWTOOTH)
            preload("miss_noise", 700.0, 100.0, 0.12, Waveform.NOISE)
            preload("clash_ring", 1800.0, 700.0, 0.09, Waveform.TRIANGLE)
            preload("clash_buzz", 220.0, 140.0, 0.1, Waveform.SAWTOOTH)
            preload("clash_thud", 140.0, 70.0, 0.13, Waveform.SINE)
            preload("light_placed", 650.0, 950.0, 0.12, Waveform.SINE)
            preload("clear_blip", 523.0, null, 0.12, Waveform.TRIANGLE)
            preload("gameover_blip", 380.0, null, 0.22, Waveform.TRIANGLE)
        }
    }

    private fun preload(name: String, freq: Double, toFreq: Double?, durationSec: Double, waveform: Waveform) {
        val pcm = PcmToneGenerator.renderTone(freq, toFreq, durationSec, waveform, volume = 1.0)
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

    /** 光が鋭く伸びる「しゅん」という短くシャープな発射音(低音の撃ち出し感は持たせず、高音のスイープのみ)。 */
    fun playFire() {
        play("fire_sweep_high", volume = 0.13)
        play("fire_sweep_mid", volume = 0.07)
        play("fire_noise", volume = 0.045)
    }

    /** ビームが的に届いた瞬間の短く軽いタッチ音(直後の破砕音の前振り)。 */
    fun playHitConfirm() {
        play("hit_tick", volume = 0.1)
    }

    /** ガラスや氷が砕け散るような、不協和な高音が重なり合う爽快な着弾音。 */
    fun playConnect(combo: Int) {
        val comboBoost = (1.0 + combo * 0.02).coerceIn(1.0, 1.6)
        play("connect_crack", volume = 0.17)
        val shards = listOf(
            Triple("connect_shard_low", 2400.0, 0.075),
            Triple("connect_shard_low", 3100.0, 0.067),
            Triple("connect_shard_low", 3900.0, 0.059),
            Triple("connect_shard_low", 4800.0, 0.051),
            Triple("connect_shard_high", 6000.0, 0.043),
        )
        shards.forEachIndexed { i, (name, targetFreq, vol) ->
            val baseFreq = if (name == "connect_shard_low") 2400.0 else 6000.0
            play(name, volume = vol, rate = targetFreq / baseFreq * comboBoost, delayMs = (i * 8).toLong())
        }
        play("connect_impact", volume = 0.13, rate = (1.0 + combo * 0.03).coerceIn(0.5, 2.0))
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
}
