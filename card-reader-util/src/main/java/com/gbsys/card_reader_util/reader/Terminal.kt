package com.cardreadergb.reader

import android.nfc.Tag
import android.nfc.tech.IsoDep

class Terminal constructor(tag: Tag) : ITransceiver {

    private var isoDep = IsoDep.get(tag)

    private fun init() {
        isoDep.timeout = 50000
        isoDep.connect()
    }

    override fun transceive(command: ByteArray): ByteArray {
        if (!isoDep.isConnected) init()
        return isoDep.transceive(command)
    }

}