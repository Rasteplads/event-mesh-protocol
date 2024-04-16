package rasteplads.api

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class MockDevice(
    override val receiveChannel: Channel<ByteArray>,
    override val transmitChannel: Channel<ByteArray>,
    override val transmissionInterval: Long
) : TransportDevice {

    val transmittedMessages: MutableList<ByteArray> = mutableListOf()
    val transmittingJob: AtomicReference<Job?> = AtomicReference(null)

    val reveivingJob: AtomicReference<Job?> = AtomicReference(null)

    val receivedPool: AtomicReference<MutableList<ByteArray>> = AtomicReference(mutableListOf())

    val transmitting: AtomicBoolean = AtomicBoolean(false)
    val receiving: AtomicBoolean = AtomicBoolean(false)

    override suspend fun beginTransmitting(message: ByteArray): Unit {
        transmittedMessages.removeAll { true }
        transmitting.set(true)

        while (transmitting.get()) {
            transmittedMessages.add(message)
            delay(transmissionInterval)
            yield()
        }
    }

    override fun stopTransmitting(): Unit = transmitting.set(false)

    override suspend fun beginReceiving(callback: suspend (ByteArray) -> Unit) {
        receiving.set(true)

        while (receiving.get()) {
            val iter = receivedPool.get().iterator()
            for (i in iter) {
                callback(i)
                iter.remove()
            }
            delay(50) // 1 sec
            yield()
        }
    }

    override fun stopReceiving(): Unit = receiving.set(false)

    fun receiveMessage(b: ByteArray) {
        receivedPool.get().add(b)
    }
}

class EventMeshDeviceTest {}
