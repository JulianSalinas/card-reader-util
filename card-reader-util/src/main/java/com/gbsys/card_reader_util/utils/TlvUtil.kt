package com.gbsys.card_reader_util.utils

import com.gbsys.card_reader_util.emvnfccard.BytesUtils
import com.gbsys.card_reader_util.emvnfccard.EmvTags
import com.gbsys.card_reader_util.emvnfccard.ITag
import com.gbsys.card_reader_util.models.TLV
import com.gbsys.card_reader_util.models.TagAndLength
import org.apache.commons.lang3.ArrayUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.experimental.and


object TlvUtil {

    /**
     * Method used to find Tag with ID
     * @param tagIdBytes the tag to find
     * @return the tag found
     */
    private fun searchTagById(tagIdBytes: ByteArray): ITag {
        return EmvTags.getNotNull(tagIdBytes) // TODO take app (IIN or RID) into consideration
    }

    private fun readTagIdBytes(stream: ByteArrayInputStream): ByteArray {
        val tagBAOS = ByteArrayOutputStream()
        val tagFirstOctet = stream.read().toByte()
        tagBAOS.write(tagFirstOctet.toInt())
        // Find TAG bytes
        val MASK = 0x1F.toByte()
        if ((tagFirstOctet and MASK).toInt() == MASK.toInt()) { // EMV book 3, Page 178 or Annex B1 (EMV4.3)

            do {
                val nextOctet = stream.read()
                if (nextOctet < 0) {
                    break
                }
                val tlvIdNextOctet = nextOctet.toByte()
                tagBAOS.write(tlvIdNextOctet.toInt())
                if (!BytesUtils.matchBitByBitIndex(
                        tlvIdNextOctet.toInt(),
                        7
                    ) || BytesUtils.matchBitByBitIndex(tlvIdNextOctet.toInt(), 7) && (tlvIdNextOctet and 0x7f.toByte()).toInt() == 0
                ) {
                    break
                }
            } while (true)
        }
        return tagBAOS.toByteArray()
    }

    private fun readTagLength(stream: ByteArrayInputStream): Int { // Find LENGTH bytes
        val length: Int
        var tmpLength = stream.read()
        if (tmpLength < 0) {
            throw Exception("Negative length: $tmpLength")
        }
        if (tmpLength <= 127) { // 0111 1111
            // short length form
            length = tmpLength
        } else if (tmpLength == 128) { // 1000 0000
            // length identifies indefinite form, will be set later
            // indefinite form is not specified in ISO7816-4,
            // but we include it here for completeness
            length = tmpLength
        } else { // long length form
            val numberOfLengthOctets = tmpLength and 127 // turn off 8th bit
            tmpLength = 0
            for (i in 0 until numberOfLengthOctets) {
                val nextLengthOctet = stream.read()
                if (nextLengthOctet < 0) {
                    throw Exception("EOS when reading length bytes")
                }
                tmpLength = tmpLength shl 8
                tmpLength = tmpLength or nextLengthOctet
            }
            length = tmpLength
        }
        return length
    }

    private fun getNextTLV(stream: ByteArrayInputStream): TLV {
        if (stream.available() < 2) {
            throw Exception("Error parsing data. Available bytes < 2 . Length=" + stream.available())
        }
        // ISO/IEC 7816 uses neither '00' nor 'FF' as tag value.
        // Before, between, or after TLV-coded data objects,
        // '00' or 'FF' bytes without any meaning may occur
        // (for example, due to erased or modified TLV-coded data objects).
        stream.mark(0)
        var peekInt = stream.read()
        var peekByte = peekInt.toByte()

        // peekInt == 0xffffffff indicates EOS
        while (peekInt != -1 && (peekByte == 0xFF.toByte() || peekByte == 0x00.toByte())) {
            stream.mark(0) // Current position
            peekInt = stream.read()
            peekByte = peekInt.toByte()
        }

        stream.reset() // Reset back to the last known position without 0x00 or 0xFF

        if (stream.available() < 2) {
            throw Exception("Error parsing data. Available bytes < 2 . Length=" + stream.available())
        }
        val tagIdBytes = readTagIdBytes(stream)

        // We need to get the raw length bytes.
        // Use quick and dirty workaround
        stream.mark(0)
        val posBefore = stream.available()

        // Now parse the lengthbyte(s)
        // This method will read all length bytes.
        // We can then find out how many bytes was read.
        var length = readTagLength(stream) // Decoded

        // Now find the raw (encoded) length bytes
        val posAfter = stream.available()
        stream.reset()
        val lengthBytes = ByteArray(posBefore - posAfter)

        if (lengthBytes.isEmpty() || lengthBytes.size > 4) {
            throw Exception("Number of length bytes must be from 1 to 4. Found " + lengthBytes.size)
        }

        stream.read(lengthBytes, 0, lengthBytes.size)
        val rawLength = BytesUtils.byteArrayToInt(lengthBytes)
        val valueBytes: ByteArray
        val tag = searchTagById(tagIdBytes)

        // Find VALUE bytes
        if (rawLength == 128) { // 1000 0000

            // indefinite form
            stream.mark(0)
            var prevOctet = 1
            var curOctet: Int
            var len = 0

            while (true) {
                len++
                curOctet = stream.read()
                if (curOctet < 0) {
                    throw Exception(
                        "Error parsing data. TLV " + "length byte indicated indefinite length, but EOS "
                                + "was reached before 0x0000 was found" + stream.available()
                    )
                }
                if (prevOctet == 0 && curOctet == 0) {
                    break
                }
                prevOctet = curOctet
            }

            len -= 2
            valueBytes = ByteArray(len)
            stream.reset()
            stream.read(valueBytes, 0, len)
            length = len

        } else {

            if (stream.available() < length) {
                throw Exception(
                    "Length byte(s) indicated " + length + " value bytes, but only " + stream.available()
                            + " " + (if (stream.available() > 1) "are" else "is") + " available"
                )
            }
            // definite form
            valueBytes = ByteArray(length)
            stream.read(valueBytes, 0, length)
        }
        // Remove any trailing 0x00 and 0xFF
        stream.mark(0)
        peekInt = stream.read()
        peekByte = peekInt.toByte()

        while (peekInt != -1 && (peekByte == 0xFF.toByte() || peekByte == 0x00.toByte())) {
            stream.mark(0)
            peekInt = stream.read()
            peekByte = peekInt.toByte()
        }

        stream.reset() // Reset back to the last known position without 0x00 or 0xFF
        return TLV(tag, length, lengthBytes, valueBytes)
    }

    /**
     * Method used to parser Tag and length
     * @param data data to parse
     * @return list of tag and length
     */
    fun parseTagAndLength(data: ByteArray?): List<TagAndLength> {
        val tagAndLengthList: MutableList<TagAndLength> = ArrayList()
        if(data == null) return tagAndLengthList
        val stream = ByteArrayInputStream(data)
        while (stream.available() > 0) {
            if (stream.available() < 2) {
                throw Exception("Data length < 2 : " + stream.available())
            }
            val tag = searchTagById(readTagIdBytes(stream))
            val tagValueLength = readTagLength(stream)
            tagAndLengthList.add(TagAndLength(tag, tagValueLength))
        }
        return tagAndLengthList
    }

    /**
     * Method used to get Tag value
     * @param pData data
     * @param pTag tag to find
     * @return tag value or null
     */
    fun getValue(pData: ByteArray?, vararg pTag: ITag): ByteArray? {
        var ret: ByteArray? = null
        if(pData == null) return ret
        val stream = ByteArrayInputStream(pData)
        while (stream.available() > 0) {
            val tlv = getNextTLV(stream)
            if (ArrayUtils.contains(pTag, tlv.tag)) {
                return tlv.valueBytes
            } else if (tlv.tag.isConstructed) {
                ret = getValue(tlv.valueBytes, *pTag)
                if (ret != null) {
                    break
                }
            }
        }
        return ret
    }

    /**
     * Method used to get length of all Tags
     * @param pList tag length list
     * @return the sum of tag length
     */
    fun getLength(pList: List<TagAndLength>?): Int {
        var ret = 0
        if (pList != null) {
            for (tl in pList) {
                ret += tl.length
            }
        }
        return ret
    }

}