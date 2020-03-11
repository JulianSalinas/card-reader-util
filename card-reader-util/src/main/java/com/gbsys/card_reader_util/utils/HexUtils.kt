package com.cardreadergb.utils


object HexUtils {

    fun bytesToHex(bytesIn: ByteArray): String {
        val stringBuilder = StringBuilder()
        for (byteOut in bytesIn)
            stringBuilder.append(String.format("%02X", byteOut))
        return stringBuilder.toString()
    }

    fun hexToBytes(hexadecimal: String): ByteArray {
        val result = ByteArray(hexadecimal.length / 2)
        for (i in hexadecimal.indices step 2) {
            val shifted = Character.digit(hexadecimal[i], 16) shl 4
            val last = Character.digit(hexadecimal[i + 1], 16)
            result[i / 2] = (shifted + last).toByte()
        }
        return result
    }

    fun hexToAscii(hexadecimal: String): String {
        val stringBuilder = StringBuilder()
        for (i in hexadecimal.indices step 2) {
            val ascii = hexadecimal.substring(i, i + 2).toInt(16).toChar()
            stringBuilder.append(ascii)
        }
        return stringBuilder.toString()
    }

    fun bytesToAscii(bytesIn: ByteArray): String {
        val hexadecimal = bytesToHex(bytesIn)
        return hexToAscii(hexadecimal)
    }

}