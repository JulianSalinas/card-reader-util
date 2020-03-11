package com.gbsys.card_reader_util.models

import android.os.Parcelable
import com.gbsys.card_reader_util.models.PayApp
import java.util.*
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PayCard (

    /**
     * Default application
     */
    val payApp: PayApp,

    /**
     * Card Holder's Complete name
     * Sometimes it is not present
     */
    var owner: String? = null,

    /**
     * Card's Number
     * It is supposed to be always present
     */
    var number: String? = null,

    /**
     * Card's Expiration Date
     * It is supposed to be always present
     */
    var expiration: Date? = null,

    /**
     * Fields to take into account
     */
    var track1: String? = null,
    var track2: String? = null

) : Parcelable