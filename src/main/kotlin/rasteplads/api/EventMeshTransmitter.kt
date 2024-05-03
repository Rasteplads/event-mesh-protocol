package rasteplads.api

import kotlinx.coroutines.*

class EventMeshTransmitter<Tx>(private val device: TransportDevice<*, Tx>) {

    var transmitTimeout: Long = 60000 // 60 sec // TODO: Default val

    fun transmit(message: ByteArray) {
        var callback: Tx? = null
        try {
            callback = device.beginTransmitting(message)
            Thread.sleep(transmitTimeout)
        } finally {
            device.stopTransmitting(callback!!)
        }
    }
}
