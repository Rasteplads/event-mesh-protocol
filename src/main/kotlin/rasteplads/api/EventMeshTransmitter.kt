package rasteplads.api

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex

class EventMeshTransmitter<Tx>(private val device: TransportDevice<*, Tx>) {

    fun transmit(message: ByteArray, timeout: Long) {
        var callback: Tx? = null
        try {
            callback = device.beginTransmitting(message)
            Thread.sleep(timeout)
        } finally {
            device.stopTransmitting(callback!!)
        }
    }
}
