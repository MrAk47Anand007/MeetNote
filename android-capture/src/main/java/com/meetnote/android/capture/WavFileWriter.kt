package com.meetnote.android.capture

import java.io.File
import java.io.RandomAccessFile

internal class WavFileWriter(
    private val outputFile: File,
    private val sampleRateHz: Int,
    private val channelCount: Int,
    private val bitsPerSample: Int
) : AutoCloseable {
    private val randomAccessFile = RandomAccessFile(outputFile, "rw")
    private var dataBytesWritten = 0L

    init {
        outputFile.parentFile?.mkdirs()
        randomAccessFile.setLength(0)
        writeHeader(dataLength = 0)
    }

    fun write(buffer: ByteArray, length: Int) {
        randomAccessFile.seek(HEADER_SIZE_BYTES + dataBytesWritten)
        randomAccessFile.write(buffer, 0, length)
        dataBytesWritten += length
    }

    override fun close() {
        writeHeader(dataLength = dataBytesWritten)
        randomAccessFile.fd.sync()
        randomAccessFile.close()
    }

    private fun writeHeader(dataLength: Long) {
        val byteRate = sampleRateHz * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8
        val riffChunkSize = HEADER_SIZE_BYTES - 8 + dataLength

        randomAccessFile.seek(0)
        randomAccessFile.writeBytes("RIFF")
        randomAccessFile.writeIntLE(riffChunkSize.toInt())
        randomAccessFile.writeBytes("WAVE")
        randomAccessFile.writeBytes("fmt ")
        randomAccessFile.writeIntLE(16)
        randomAccessFile.writeShortLE(1)
        randomAccessFile.writeShortLE(channelCount)
        randomAccessFile.writeIntLE(sampleRateHz)
        randomAccessFile.writeIntLE(byteRate)
        randomAccessFile.writeShortLE(blockAlign)
        randomAccessFile.writeShortLE(bitsPerSample)
        randomAccessFile.writeBytes("data")
        randomAccessFile.writeIntLE(dataLength.toInt())
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        writeByte(value and 0xff)
        writeByte(value shr 8 and 0xff)
        writeByte(value shr 16 and 0xff)
        writeByte(value shr 24 and 0xff)
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        writeByte(value and 0xff)
        writeByte(value shr 8 and 0xff)
    }

    private companion object {
        const val HEADER_SIZE_BYTES = 44L
    }
}
