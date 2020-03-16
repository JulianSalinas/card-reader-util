package com.gbsys.card_reader_util.models

import com.gbsys.card_reader_util.emvnfccard.ITag


data class TLV(

    val tag: ITag,

    val length: Int,

    val rawEncodedLengthBytes: ByteArray,

    val valueBytes:  ByteArray?

) {

    val tagBytes get() = tag.tagBytes

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TLV) return false
        other as TLV
        if (tag != other.tag) return false
        return true
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }

}
