package com.cardreadergb.reader

interface ITransceiver {
    fun transceive(command: ByteArray): ByteArray
}