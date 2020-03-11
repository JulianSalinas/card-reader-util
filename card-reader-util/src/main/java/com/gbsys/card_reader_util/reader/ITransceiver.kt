package com.gbsys.card_reader_util.reader

interface ITransceiver {
    fun transceive(command: ByteArray): ByteArray
}