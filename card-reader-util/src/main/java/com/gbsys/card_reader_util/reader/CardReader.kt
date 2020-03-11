package com.gbsys.card_reader_util.reader

import android.util.Log
import com.gbsys.card_reader_util.models.PayCard
import com.gbsys.card_reader_util.utils.EmvUtils
import com.gbsys.card_reader_util.utils.HexUtils
import com.gbsys.card_reader_util.utils.TlvUtil
import com.gbsys.card_reader_util.utils.TrackUtils
import com.github.devnied.emvnfccard.enums.CommandEnum
import com.github.devnied.emvnfccard.enums.SwEnum
import com.github.devnied.emvnfccard.iso7816emv.EmvTags
import com.github.devnied.emvnfccard.model.Afl
import com.github.devnied.emvnfccard.utils.CommandApdu
import com.github.devnied.emvnfccard.utils.ResponseUtils
import org.apache.commons.lang3.ArrayUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception


class CardReader constructor(private val terminal: Terminal) {

    private var paycard: PayCard? = null

    /**
     * 1. Extract the application details by using AppProvider
     * 2. Get the Processing Options Data Object List (PDOL)
     * 3. Extract card information
     */
    fun doRead() : PayCard {

        val provider = AppProvider(terminal)

        val pDol = TlvUtil.getValue(provider.fci, EmvTags.PDOL)

        var gpo = getProcessingOptions(pDol)

        // GPO Not found. Second try
        if (!ResponseUtils.isSucceed(gpo)) {
            gpo = getProcessingOptions(null)
            if (!ResponseUtils.isSucceed(gpo)) throw Exception("GPO Not Found")
        }

        paycard = PayCard(
            payApp = provider.getDefaultApplication()
        )

        if(!getCommonCardData(gpo))
            throw Exception("Card could Not be Read")

        return paycard as PayCard

    }

    /**
     * Get <GPO> Processing Options
     * @param pPdol <PDOL> Processing Options Data Object List
     */
    private fun getProcessingOptions(pPdol: ByteArray?): ByteArray? {
        val list = TlvUtil.parseTagAndLength(pPdol)
        val out = ByteArrayOutputStream()
        try {
            out.write(byteArrayOf(0x83.toByte()))
            out.write(TlvUtil.getLength(list))
            for (tl in list) out.write(EmvUtils.constructValue(tl))
        }
        catch (ioe: IOException) {
            Log.e("GPO", "Construct GPO Command:" + ioe.message)
        }
        return terminal.transceive(EmvUtils.gpoCommand(out.toByteArray()))
    }

    private fun getCommonCardData(pGpo: ByteArray?): Boolean {
        var ret = false
        // Extract data from Message Template 1
        var data = TlvUtil.getValue(pGpo, EmvTags.RESPONSE_MESSAGE_TEMPLATE_1)
        if (data != null) {
            data = ArrayUtils.subarray(data, 2, data.size)
        } else { // Extract AFL data from Message template 2
            ret = TrackUtils.extractTrack2Data(paycard as PayCard, pGpo)
            if (!ret) {
                data = TlvUtil.getValue(pGpo, EmvTags.APPLICATION_FILE_LOCATOR)
            } else {
                getCardHolder(pGpo)
            }
        }
        if (data != null) { // Extract Afl

            val listAfl: List<Afl> = getAppFileLocator(data) ?: return false

            // for each AFL
            for (afl in listAfl) { // check all records
                for (index in afl.firstRecord..afl.lastRecord) {
                    var info: ByteArray = terminal.transceive(
                        CommandApdu(
                            CommandEnum.READ_RECORD,
                            index,
                            afl.sfi shl 3 or 4,
                            0
                        ).toBytes()
                    )
                    if (ResponseUtils.isEquals(info, SwEnum.SW_6C)) {
                        info = terminal.transceive(
                            CommandApdu(
                                CommandEnum.READ_RECORD, index, afl.sfi shl 3 or 4,
                                info[info.size - 1].toInt()
                            ).toBytes()
                        )
                    }
                    // Extract card data
                    if (ResponseUtils.isSucceed(info)) {
                        getCardHolder(info)
                        paycard?.track1 = TrackUtils.getTrack1Data(info)
                        paycard?.track2 = TrackUtils.getTrack2Data(info)
                        if (TrackUtils.extractTrack2Data(paycard as PayCard, info)) {
                            return true
                        }
                    }
                }
            }
        }
        return ret
    }

    /**
     * Get <AFL> Application File Locators
     * @param pAfl Raw data to extract the <ALF>
     */
    private fun getAppFileLocator(pAfl: ByteArray?): List<Afl>? {
        val list: MutableList<Afl> = ArrayList()
        val stream = ByteArrayInputStream(pAfl)
        while (stream.available() >= 4) {
            val afl = Afl()
            afl.sfi = stream.read() shr 3
            afl.firstRecord = stream.read()
            afl.lastRecord = stream.read()
            afl.isOfflineAuthentication = stream.read() == 1
            list.add(afl)
        }
        return list
    }

    /**
     * Get cardholder from raw data
     * @param pData It could be extracted from <GPO> or <Track2> raw data
     */
    private fun getCardHolder(pData: ByteArray?) {
        val name = TlvUtil.getValue(pData, EmvTags.CARDHOLDER_NAME)
        paycard?.owner = if(name != null) HexUtils.bytesToAscii(name) else "Desconocido"
    }

}