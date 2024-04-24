package rasteplads.api

import kotlinx.coroutines.*

class EventMeshTransmitter(private val device: TransportDevice) {

    var transmitTimeout: Long = 60000 // 60 sec // TODO: Default val
    var coroutineScope = CoroutineScope(CoroutineName("EventMeshTransmitterScope"))

    fun transmit(message: ByteArray) =
        coroutineScope.launch {
            try {
                device.beginTransmitting(message)
                delay(transmitTimeout)
            } finally {
                device.stopTransmitting()
            }
        }
}
