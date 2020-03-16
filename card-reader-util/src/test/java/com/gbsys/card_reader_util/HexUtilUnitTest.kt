package com.gbsys.card_reader_util

import com.gbsys.card_reader_util.utils.HexUtils
import org.junit.Test
import org.junit.Assert.*

class HexUtilUnitTest {

    // Results got from rapidtables.com/convert/number/ascii-to-hex.html
    @Test
    fun bytesToHexTest() {
        val bytes = "Julian".toByteArray()
        val hex = HexUtils.bytesToHex(bytes)
        assertEquals(hex, "4A756C69616E")
    }

    @Test
    fun hexToBytesTest() {
        val hex = "4A756C69616E"
        val name = "Julian".toByteArray()
        val bytes = HexUtils.hexToBytes(hex)
        assertEquals(HexUtils.hexToAscii(hex), HexUtils.bytesToAscii(bytes))
        assertEquals(HexUtils.bytesToAscii(name), HexUtils.bytesToAscii(bytes))
    }

    @Test
    fun hexToAsciiTest() {
        val hex = "4A756C69616E"
        val ascii = HexUtils.hexToAscii(hex)
        assertEquals(ascii, "Julian")
    }

    @Test
    fun bytesToAsciiTest() {
        val bytes = "Julian".toByteArray()
        val name = HexUtils.bytesToAscii(bytes)
        assertEquals(name, "Julian")
    }

}
