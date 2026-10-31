package com.laserarrows.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class SignatureSfxTest {
    private val sounds = buildMap {
        repeat(SignatureSfx.TAP_VARIANTS) { put("tap_$it", SignatureSfx.renderTap(it)) }
        put("hit_touch", SignatureSfx.renderHitTouch())
        put("heart_arrival", SignatureSfx.renderHeartArrival())
    }

    @Test
    fun soundsAreStereoNormalizedAndEndSilently() {
        sounds.forEach { (name, pcm) ->
            assertEquals("$name はステレオの偶数長", 0, pcm.size % 2)
            val peak = pcm.maxOf { abs(it.toInt()) }
            assertTrue("$name のピークが-1dBFS付近で、クリップしていない: $peak", peak in 28000..29500)
            val tail = pcm.takeLast(20).maxOf { abs(it.toInt()) }
            assertTrue("$name の末尾が無音まで消えている: $tail", tail < 200)
        }
    }

    /** 試聴用に build/sfx-preview/ へWAVを書き出す(PCで再生して確認できる)。 */
    @Test
    fun exportPreviews() {
        val dir = File("build/sfx-preview").apply { mkdirs() }
        sounds.forEach { (name, pcm) ->
            File(dir, "$name.wav").writeBytes(WavEncoder.encode(pcm, SignatureSfx.SAMPLE_RATE, channels = 2))
        }
    }
}
