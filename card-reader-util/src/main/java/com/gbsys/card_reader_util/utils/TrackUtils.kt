package com.cardreadergb.utils

import android.util.Log
import com.gbsys.card_reader_util.models.PayCard
import com.cardreadergb.utils.TlvUtil.getValue
import com.github.devnied.emvnfccard.iso7816emv.EmvTags
import org.apache.commons.lang3.time.DateUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


object TrackUtils {

    /**
     * Track 2 pattern
     */
    private val TRACK2_PATTERN = Pattern.compile("([0-9]{1,19})D([0-9]{4})([0-9]{3})?(.*)")

    fun extractTrack2Data(paycard: PayCard, pData: ByteArray?): Boolean {

        val track2 = getValue(pData, EmvTags.TRACK_2_EQV_DATA, EmvTags.TRACK2_DATA) ?: return false

        val data = HexUtils.bytesToHex(track2)
        val m: Matcher = TRACK2_PATTERN.matcher(data)

        // Check pattern
        if (m.find()) {
            paycard.number = m.group(1)
            val formatter = SimpleDateFormat("yyMM", Locale.getDefault())
            try {
                paycard.expiration = DateUtils.truncate(formatter.parse(m.group(2)), Calendar.MONTH)
            }
            catch (e: ParseException) {
                Log.e("TrackUtils", "Unable to parse the card's expiration: " +  e.message)
                return false
            }
            return true
        }

        return false
    }

    fun getTrack1Data(pData: ByteArray?): String? {
        val track = getValue(pData, EmvTags.TRACK_2_EQV_DATA, EmvTags.TRACK1_DATA)
        return if (track != null) HexUtils.bytesToHex(track) else null
    }

    fun getTrack2Data(pData: ByteArray?): String? {
        val track2 = getValue(pData, EmvTags.TRACK_2_EQV_DATA, EmvTags.TRACK2_DATA)
        return if (track2 != null) HexUtils.bytesToHex(track2) else null
    }

}