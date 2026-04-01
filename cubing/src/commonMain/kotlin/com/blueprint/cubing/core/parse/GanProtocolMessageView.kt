package com.blueprint.cubing.core.parse

class GanProtocolMessageView(val message: ByteArray) {
    val messageLength = message.size * 8

    /**
     * Возвращает числовое значение битового слова длиной bitLength, начиная с startBit.
     * littleEndian указывает порядок байтов внутри слова (по-умолчанию big-endian).
     * Поддерживаются длины: <=8, 16 или 32.
     */
    fun getBitWord(startBit: Int, bitLength: Int, littleEndian: Boolean = false): Long {
        require(startBit >= 0) { "startBit must be non-negative" }
        require(bitLength > 0) { "bitLength must be positive" }
        require(bitLength <= 8 || bitLength == 16 || bitLength == 32) { "Unsupported bit word length: $bitLength" }
        require(startBit + bitLength <= messageLength) { "Requested bits out of range: $startBit..${messageLength}" }
        return getBits(message, startBit, bitLength)
    }

    private fun getBits(message: ByteArray, startBit: Int, bitLength: Int): Long {
        val startBytePos = startBit / 8
        val startBitOffset = startBit % 8
        val endBytePos = (startBit + bitLength - 1) / 8
        val endBitOffset = (startBit + bitLength - 1) % 8

        var value: Long = 0

        for (i in startBytePos .. endBytePos) {
            val b = message[i].toLong() and 0xFF
            value = (value shl 8) or b
        }

        value = value ushr (7 - endBitOffset)
        value = value shl (64 - (bitLength))
        value = value ushr (64 - (bitLength))

        return value
    }
}