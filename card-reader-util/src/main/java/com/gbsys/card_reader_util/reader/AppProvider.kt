package com.gbsys.card_reader_util.reader

import com.gbsys.card_reader_util.models.Network
import com.gbsys.card_reader_util.models.PayApp
import com.gbsys.card_reader_util.models.Technology
import com.gbsys.card_reader_util.utils.EmvUtils
import com.gbsys.card_reader_util.utils.HexUtils
import java.lang.Exception


class AppProvider constructor(private val terminal: Terminal) {

    val ppse = getProximityEnvironment()
    val id = getAppId(ppse)
    val fci = getFileControlInformation(id)

    fun getDefaultApplication() : PayApp {

        val rid = getProviderId(id)
        val pix = getExtension(id)
        val network = getNetwork(rid)

        return PayApp(
            id = HexUtils.bytesToHex(id),
            rid = HexUtils.bytesToHex(rid),
            pix = HexUtils.bytesToHex(pix),
            network = network,
            label = getAppLabel(fci),
            technology = getTechnology(network)
        )

    }

    /**
     * Get the <PPSE> Proximity Payment System Environment
     */
    private fun getProximityEnvironment(): ByteArray {
        val command: ByteArray = EmvUtils.ppseCommand()
        val ppse = terminal.transceive(command)
        if (!EmvUtils.isOk(ppse)) throw Exception("PPSE Not Found")
        return ppse
    }

    /**
     * First, the bytes are read until the <AID_TLV_TAG> is found.
     * The next byte after <AID_TLV_TAG> is the AID's lenght.
     * @param ppse <PPSE> Proximity Payment System Environment
     * @return <AID> Application Identifier
     */
    private fun getAppId(ppse: ByteArray) : ByteArray {

        val aidTlvTag = byteArrayOf(0x4F.toByte())

        for (i in ppse.indices){
            val aidTlvTagLength = ppse.copyOfRange(i, i + aidTlvTag.size)
            if (aidTlvTag.contentEquals(aidTlvTagLength))
                return ppse.copyOfRange(i + 2, i + 2 + ppse[i + 1])
        }

        throw Exception("AID Not Found")
    }

    /**
     * The first 5 bytes are the <RID> accordeing to
     * @see <a href="https://en.wikipedia.org/wiki/EMV"/>
     */
    private fun getProviderId(id: ByteArray) : ByteArray {
        return id.copyOfRange(0, 5)
    }

    /**
     * The last digits are the <PIX> are used by each provider
     * to identify their own cards according to
     * @see <a href="https://en.wikipedia.org/wiki/EMV"/>
     */
    private fun getExtension(id: ByteArray) : ByteArray {
        return id.copyOfRange(5, id.size)
    }

    /**
     * Get the card's network
     * @param rid <RID> Application Provider Identifier
     * @return <AppLabel> Mastercard | Visa | American Express
     */
    private fun getNetwork(rid: ByteArray) : String? {
        val ridHex = HexUtils.bytesToHex(rid)
        return Network.TABLE[ridHex]
    }

    /**
     * Get the card's contactless technology
     * @param network <AppLabel> Mastercard | Visa | American Express
     * @return <TECH> Paypass | Paywave | Expresspay
     */
    private fun getTechnology(network: String?) : String? {
        return if(network != null) Technology.TABLE[network] else null
    }

    /**
     * Get the <FCIT> File Control Information Template
     * @param appId <AID> Application Identifier
     */
    private fun getFileControlInformation(appId: ByteArray) : ByteArray {
        val ppse: ByteArray = EmvUtils.appIdCommand(appId)
        val fcit = terminal.transceive(ppse)
        if (!EmvUtils.isOk(fcit)) throw Exception("FCIT Not Found")
        return fcit
    }

    /**
     * Gets the Applications's Label present in the card
     * @param fcit <FCI> File Control Information Template
     * @return appLabel Ex. "Debit Mastercard"
     */
    private fun getAppLabel(fcit: ByteArray): String? {
        val tag = byteArrayOf(0x50.toByte())
        val appLabel = EmvUtils.getTlvValue(fcit, tag)
        return if(appLabel != null) HexUtils.bytesToAscii(appLabel) else null
    }

}