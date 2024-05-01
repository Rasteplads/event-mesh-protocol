package rasteplads.api

import kotlinx.coroutines.*

class EventMeshTransmitter(private val device: TransportDevice) {

    var transmitTimeout: Long = 60000 // 60 sec // TODO: Default val

    fun transmit(message: ByteArray) {
        try {
            device.beginTransmitting(message)
            Thread.sleep(transmitTimeout)
        } finally {
            device.stopTransmitting()
        }
    }
}
