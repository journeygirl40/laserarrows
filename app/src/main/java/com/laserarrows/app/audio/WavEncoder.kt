package com.laserarrows.app.audio

import java.io.ByteArrayOutputStream

/** 16bitモノラルPCMを、SoundPoolがロードできる最小限のWAV(RIFF)バイト列へ変換する。 */
object WavEncoder {
    fun encode(pcm: ShortArray, sampleRate: Int = 44100): ByteArray {
        val dataSize = pcm.size * 2
        val out = ByteArrayOutputStream(44 + dataSize)

        fun writeString(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun writeIntLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v ushr 8) and 0xFF)
            out.write((v ushr 16) and 0xFF)
            out.write((v ushr 24) and 0xFF)
        }
        fun writeShortLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v ushr 8) and 0xFF)
        }

        writeString("RIFF")
        writeIntLE(36 + dataSize)
        writeString("WAVE")
        writeString("fmt ")
        writeIntLE(16)
        writeShortLE(1) // PCM
        writeShortLE(1) // mono
        writeIntLE(sampleRate)
        writeIntLE(sampleRate * 2) // byte rate (sampleRate * channels * bytesPerSample)
        writeShortLE(2) // block align
        writeShortLE(16) // bits per sample
        writeString("data")
        writeIntLE(dataSize)
        for (s in pcm) writeShortLE(s.toInt())

        return out.toByteArray()
    }
}
