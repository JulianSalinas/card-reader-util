package com.gbsys.card_reader_util.models

import com.github.devnied.emvnfccard.iso7816emv.ITag
import java.util.*

data class TagAndLength(
    val tag: ITag,
    val length: Int
) {

    val bytes: ByteArray get() {
        val tagBytes = tag.tagBytes
        val tagAndLengthBytes =
            Arrays.copyOf(tagBytes, tagBytes.size + 1)
        tagAndLengthBytes[tagAndLengthBytes.size - 1] = length.toByte()
        return tagAndLengthBytes
    }

    override fun toString(): String {
        return "$tag length: $length"
    }

}
