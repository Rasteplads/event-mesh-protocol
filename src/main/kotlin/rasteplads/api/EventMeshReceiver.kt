package rasteplads.api

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import rasteplads.api.EventMesh.Companion.ID_MAX_SIZE

class EventMeshReceiver(private val device: TransportDevice) {
    val handlers = mutableListOf<suspend (ByteArray) -> Unit>()
    val handlersA: AtomicReference<MutableList<suspend (ByteArray) -> Unit>> =
        AtomicReference(mutableListOf())
    var duration: Long = 10000 // 10 sec //TODO: Default val
    private val scannerCount: AtomicInteger = AtomicInteger(0)

    suspend fun scanForID(id: ByteArray, callback: suspend () -> Unit) {
        val callbackWrap: suspend (ByteArray) -> Unit = { msg: ByteArray ->
            if (id.zip(msg.sliceArray(1..ID_MAX_SIZE)).all { (i, s) -> i == s }) {
                callback()
            }
        }
        try {
            handlersA.get().add(callbackWrap)
            if (scannerCount.getAndIncrement() == 0)
                device.beginReceiving(::scanForMessagesCallback)
            /*
            device.beginReceiving { scanned ->
                if (id.zip(scanned.sliceArray(1..ID_MAX_SIZE)).all { (i, s) -> i == s }) {
                    callback()
                }
            }
             */
        } finally {
            handlersA.get().remove(callbackWrap)
            if (scannerCount.decrementAndGet() == 0) device.stopReceiving()
        }
    }

    fun scanForMessages() = runBlocking {
        try {
            if (scannerCount.getAndIncrement() == 0)
                device.beginReceiving(::scanForMessagesCallback)
            withTimeout(duration) {
                // device.beginReceiving(callback)
            }
        } finally {
            if (scannerCount.decrementAndGet() == 0) device.stopReceiving()
        }
    }

    private suspend fun scanForMessagesCallback(msg: ByteArray) =
        handlersA.get().forEach { it(msg) }
}
