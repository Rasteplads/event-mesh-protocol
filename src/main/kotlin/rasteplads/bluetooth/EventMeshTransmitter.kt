package rasteplads.bluetooth

import kotlinx.coroutines.*

class EventMeshTransmitter(private val device: TransportDevice) {

    var transmitTimeout: Long = 60000 // 60 sec // TODO: Default val

    fun transmit(message: ByteArray) = runBlocking {
        try {
            withTimeout(transmitTimeout) { device.beginTransmitting(message) }
        } finally {
            device.stopTransmitting()
        }
    }
}
