package com.gbsys.card_reader_util.models

object Network {

    const val MASTERCARD = "MasterCard"
    const val VISA = "Visa"
    const val AMERICAN_EXPRESS = "American Express"

    val TABLE = mapOf(
        "A000000004" to MASTERCARD,
        "A000000003" to VISA,
        "A000000025" to AMERICAN_EXPRESS
    )

}