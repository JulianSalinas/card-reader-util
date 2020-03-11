package com.gbsys.card_reader_util.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PayApp(

    /**
     * <AID> Application Identifier
     * Composed by <RID> and <PIX>
     * @see <a href="https://en.wikipedia.org/wiki/EMV"/>
     */
    val id: String,

    /**
     * <RID> Application Provider Identifier
     * Issued by the ISO/IEC 7816-5 registration authority
     */
    val rid: String,

    /**
     * <PIX> Proprietary Application Identifier Extension
     * To differentiate among the different applications offered
     */
    val pix: String,

    /**
     * Network's name
     * EX. VISA | MASTERCARD | AMERICAN EXPRESS
     */
    val network: String?,

    /**
     * Human readable <AID>
     * DEDIT MASTERCARD | VISA CREDIT
     */
    val label: String?,

    /**
     * Contactless technology implemented
     * MASTERCARD -> PAYPASS
     * VISA -> PAYWAVE
     * AMERICAN EXPRESS -> EXPRESSPAY
     */
    val technology: String?,

    /**
     * Country where the card was issued
     */
    val country: String? = null


) : Parcelable