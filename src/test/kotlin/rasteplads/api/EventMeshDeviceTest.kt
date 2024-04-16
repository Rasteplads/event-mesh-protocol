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

    private val receivedPool: MutableList<ByteArray> = mutableListOf()

    val transmitting: AtomicBoolean = AtomicBoolean(false)

    override suspend fun beginTransmitting(message: ByteArray): Unit {
        transmittedMessages.removeAll { true }
        transmitting.set(true)

        while (transmitting.get()) {
            transmittedMessages.add(message)
            withContext(Dispatchers.IO) { Thread.sleep(transmissionInterval) }
            yield()
        }
    }

    override fun stopTransmitting(): Unit = transmitting.set(false)

    override fun beginReceiving(callback: suspend (ByteArray) -> Unit) = runBlocking {
        reveivingJob.set(
            GlobalScope.launch {
                try {
                    while (isActive) {
                        val iter = receivedPool.iterator()
                        for (i in iter) {
                            iter.remove()
                        }
                        delay(1000) // 1 sec
                    }
                } finally {
                    reveivingJob.set(null)
                }
            })
    }

    override fun stopReceiving(): Unit = runBlocking { reveivingJob.get()?.cancelAndJoin() }

    fun receiveMessage(b: ByteArray) = receivedPool.add(b)
}

class EventMeshDeviceTest {}
