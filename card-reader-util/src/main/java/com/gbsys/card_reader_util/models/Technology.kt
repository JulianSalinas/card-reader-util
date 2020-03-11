package com.gbsys.card_reader_util.models

object Technology {

    const val PAYPASS = "PayPass"
    const val PAYWAVE = "PayWave"
    const val EXPRESSPAY = "ExpressPay"

    val TABLE = mapOf(
        Network.MASTERCARD to PAYPASS,
        Network.VISA to PAYWAVE,
        Network.AMERICAN_EXPRESS to EXPRESSPAY
    )

}