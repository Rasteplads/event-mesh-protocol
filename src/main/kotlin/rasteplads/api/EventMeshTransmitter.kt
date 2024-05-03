package rasteplads.api

import kotlinx.coroutines.*

class EventMeshTransmitter<TTx>(private val device: TransportDevice<*, TTx>) {

    var transmitTimeout: Long = 60000 // 60 sec // TODO: Default val

    fun transmit(message: ByteArray) {
        var cb: TTx? = null
        try {
            cb = device.beginTransmitting(message)
            Thread.sleep(transmitTimeout)
        } finally {
            if (cb != null)
                device.stopTransmitting(cb)
        }
    }
}
