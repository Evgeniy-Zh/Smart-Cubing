package com.blueprint.cubing

import com.blueprint.cubing.core.logic.toKociembaFacelets
import com.blueprint.cubing.core.parse.GanProtocolMessageView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Created on 25.02.2026.
 */
class MessageParserTest {

    @Test
    fun getMessageBits1() {
        val message = byteArrayOf(0b10101010.toByte(), 0b11001100.toByte())
        val messageView = GanProtocolMessageView(message)
        val bits = messageView.getBitWord(0, 16)
        val res = bits.toBinaryString()
        assertEquals("1010101011001100", res)
    }

    @Test
    fun getMessageBits2() {
        val message = byteArrayOf(0b10101010.toByte(), 0b11001100.toByte())
        val messageView = GanProtocolMessageView(message)
        val bits = messageView.getBitWord(4, 8)
        val res = bits.toBinaryString()
        assertEquals("10101100", res)
    }

    @Test
    fun getMessageBits3() {
        val message = byteArrayOf(0b10101010.toByte(), 0b11001100.toByte())
        val messageView = GanProtocolMessageView(message)
        val bits = messageView.getBitWord(12, 4)
        val res = bits.toBinaryString()
        assertEquals("1100", res)
    }

    @Test
    fun getMessageBits4() {
        val message = byteArrayOf(0b10101010.toByte(), 0b11001100.toByte(), 0b11110000.toByte())
        val messageView = GanProtocolMessageView(message)
        val bits = messageView.getBitWord(9, 8)
        val res = bits.toBinaryString()
        assertEquals("10011001", res)
    }

    @Test
    fun getMessageBits5() {
        val message = byteArrayOf(0b10101010.toByte(), 0b11001100.toByte(), 0b11110000.toByte())
        val messageView = GanProtocolMessageView(message)

        assertFailsWith(IllegalArgumentException::class) {
            val bits = messageView.getBitWord(4, 20)
            val res = bits.toBinaryString()
            assertEquals("10101100110011110000", res)
        }
    }

    @Test
    fun getMessageBits6() {
        val message = byteArrayOf(0b10101010.toByte(), 0b11001100.toByte(), 0b11110000.toByte())
        val messageView = GanProtocolMessageView(message)
        assertFailsWith(IllegalArgumentException::class) {
            val bits = messageView.getBitWord(2, 22)
            val res = bits.toBinaryString()
            assertEquals("1010101100110011110000", res)
        }
    }

    @Test
    fun convertToMoveSequence1() {
        val CP = Array(8) { it }
        val CO = Array(8) { 0 }
        val EP = Array(12) { it }
        val EO = Array(12) { 0 }

        val result = toKociembaFacelets(CP, CO, EP, EO)
        assertEquals("UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB", result)
    }

    @Test
    fun convertToMoveSequence2() {
        val CP = arrayOf(0, 5, 2, 1, 7, 4, 6, 3)
        val CO = arrayOf(1, 2, 0, 2, 1, 1, 0, 2)
        val EP = arrayOf(1, 9, 2, 3, 11, 8, 6, 7, 4, 5, 10, 0)
        val EO = arrayOf(1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0)

        val result = toKociembaFacelets(CP, CO, EP, EO)
        assertEquals("UUFUUFLLFUUURRRRRRFFRFFDFFDRRBDDBDDBLLDLLDLLDLBBUBBUBB", result)
    }

    private fun Long.toBinaryString(): String {
        val binaryString = this.toString(2)
        return binaryString
    }
}