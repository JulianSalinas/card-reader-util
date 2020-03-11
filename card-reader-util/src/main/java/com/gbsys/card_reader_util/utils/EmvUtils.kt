package com.cardreadergb.utils

import com.github.devnied.emvnfccard.iso7816emv.EmvTags
import com.github.devnied.emvnfccard.iso7816emv.TagAndLength
import com.github.devnied.emvnfccard.iso7816emv.TerminalTransactionQualifiers
import com.github.devnied.emvnfccard.model.enums.CountryCodeEnum
import com.github.devnied.emvnfccard.model.enums.CurrencyEnum
import com.github.devnied.emvnfccard.model.enums.TransactionTypeEnum
import fr.devnied.bitlib.BytesUtils
import org.apache.commons.lang3.StringUtils
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

object EmvUtils {

    private val random: SecureRandom = SecureRandom()

    /**
     * Select command
     */
    private val SELECT = byteArrayOf(
        0x00.toByte(), // Cla
        0xA4.toByte(), // Ins
        0x04.toByte(), // P1
        0x00.toByte()  // P2
    )

    /**
     * Read Command
     */
    private val READ_RECORD = byteArrayOf(
        0x00.toByte(), // Cla
        0xB2.toByte(), // Ins
        0x5F.toByte(), // P1
        0x20.toByte()  // P2
    )

    /**
     * Get Processing Options
     */
    private val GPO = byteArrayOf(
        0x80.toByte(), // Cla
        0xA8.toByte(), // Ins
        0x00.toByte(), // P1
        0x00.toByte()  // P2
    )

    /**
     * Get Command
     */
    private val GET_DATA = byteArrayOf(
        0x80.toByte(), // Cla
        0xCA.toByte(), // Ins
        0x00.toByte(), // P1
        0x00.toByte()  // P2
    )

    /**
     * Used to verify responses
     */
    private val STATUS_WORDS = byteArrayOf(
        0x90.toByte(), // S!
        0x00.toByte()  // S2
    )

    /**
     * <APDU> Application Protocol Data Unit
     * Gets the response status words (bytes)
     */
    private fun getStatusWords(bytesIn: ByteArray): ByteArray? {
        if (bytesIn.size < 2) return null
        return bytesIn.copyOfRange(bytesIn.size - 2, bytesIn.size)
    }

    /**
     * APDU Application Protocol Data Unit
     * Checks if response succeed
     */
    fun isOk(bytesIn: ByteArray): Boolean {
        val result = getStatusWords(bytesIn)
        return result != null && Arrays.equals(result, STATUS_WORDS)
    }

    /**
     * Select commando for <PPSE>
     * Proximity Payment System Environment
     */
    fun ppseCommand(): ByteArray {
        val ppse = "2PAY.SYS.DDF01".toByteArray()
        val output = ByteArrayOutputStream()
        output.write(SELECT)
        output.write(byteArrayOf(ppse.size.toByte())) //Lc
        output.write(ppse) // Data
        output.write(byteArrayOf(0x00.toByte())) // Le
        output.close()
        return output.toByteArray()
    }

    /**
     * Processing Options Data Object List (PDOL)
     */
    fun gpoCommand(data: ByteArray?): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(GPO) // 80 A8 00 00
        output.write(byteArrayOf(data?.size?.toByte() ?: 0x00.toByte())) //Lc
        output.write(data ?: byteArrayOf(0x00.toByte())) // Data
        output.write(byteArrayOf(0x00.toByte())) // Le
        output.close()
        return output.toByteArray()
    }

    /**
     * Select Command for <AID>
     * Application Identification
     */
    fun appIdCommand(appId: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(SELECT)
        output.write(byteArrayOf(appId.size.toByte())) // Lc
        output.write(appId) // Data
        output.write(byteArrayOf(0x00.toByte())) // Le
        output.close()
        return output.toByteArray()
    }

    fun getTlvValue(bytes: ByteArray, tlvTag: ByteArray): ByteArray? {

        val index = bytes.indices.find { i ->
            val nextTag = bytes.copyOfRange(i, i + tlvTag.size)
            tlvTag.contentEquals(nextTag)
        } ?: return null

        val from = index + 1 + tlvTag.size
        val to = index + 1 + tlvTag.size  + bytes[index + tlvTag.size]
        return bytes.copyOfRange(from, to)
    }

    /**
     * Method used to construct value from tag and length
     * @param pTagAndLength tag and length value
     * @return tag value in byte
     */
    fun constructValue(pTagAndLength: TagAndLength): ByteArray {

        val ret = ByteArray(pTagAndLength.length)
        var value: ByteArray? = null

        when {

            pTagAndLength.tag === EmvTags.TERMINAL_TRANSACTION_QUALIFIERS -> {
                val terminalQual = TerminalTransactionQualifiers()
                terminalQual.setContactlessEMVmodeSupported(true)
                terminalQual.setReaderIsOfflineOnly(true)
                value = terminalQual.bytes
            }

            pTagAndLength.tag === EmvTags.TERMINAL_COUNTRY_CODE -> {
                value = BytesUtils.fromString(
                    StringUtils.leftPad(
                        CountryCodeEnum.US.numeric.toString(), pTagAndLength.length * 2,
                        "0"
                    )
                )
            }

            pTagAndLength.tag === EmvTags.TRANSACTION_CURRENCY_CODE -> {
                value = BytesUtils.fromString(
                    StringUtils.leftPad(
                        CurrencyEnum.EUR.isoCodeNumeric.toString(),
                        pTagAndLength.length * 2, "0"
                    )
                )
            }

            pTagAndLength.tag === EmvTags.TRANSACTION_DATE -> {
                val sdf = SimpleDateFormat("yyMMdd", Locale.US)
                value = BytesUtils.fromString(sdf.format(Date()))
            }

            pTagAndLength.tag === EmvTags.TRANSACTION_TYPE -> {
                value = byteArrayOf(TransactionTypeEnum.PURCHASE.key.toByte())
            }

            pTagAndLength.tag === EmvTags.AMOUNT_AUTHORISED_NUMERIC -> {
                value = BytesUtils.fromString("00")
            }

            pTagAndLength.tag === EmvTags.TERMINAL_TYPE -> {
                value = byteArrayOf(0x22)
            }

            pTagAndLength.tag === EmvTags.TERMINAL_CAPABILITIES -> {
                value = byteArrayOf(0xE0.toByte(), 0xA0.toByte(), 0x00)
            }

            pTagAndLength.tag === EmvTags.ADDITIONAL_TERMINAL_CAPABILITIES -> {
                value =
                    byteArrayOf(0x8e.toByte(), 0.toByte(), 0xb0.toByte(), 0x50, 0x05)
            }

            pTagAndLength.tag === EmvTags.DS_REQUESTED_OPERATOR_ID -> {
                value = BytesUtils.fromString("7345123215904501")
            }

            pTagAndLength.tag === EmvTags.UNPREDICTABLE_NUMBER -> {
                random.nextBytes(ret)
            }

        }

        if (value != null) {
            System.arraycopy(value, 0, ret, 0, value.size.coerceAtMost(ret.size))
        }

        return ret
    }

}
