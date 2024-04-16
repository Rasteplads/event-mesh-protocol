package rasteplads.api

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*

// TODO: Handling of timeouts should be reconsidered (or maybe implement a counter of sorts)

class EventMeshReceiver(private val device: TransportDevice) {
    var duration: Long = 10_000 // 10 sec //TODO: Default val
    private val scannerCount: AtomicInteger = AtomicInteger(0)
    private var runner: AtomicReference<Job?> = AtomicReference(null)
    private val callback: AtomicReference<suspend (ByteArray) -> Unit> = AtomicReference {}
    // (scanMessage callback, scanForId callback)
    private val handle:
        Pair<
            AtomicReference<suspend (ByteArray) -> Unit?>,
            AtomicReference<suspend (ByteArray) -> Unit?>> =
        Pair(AtomicReference(null), AtomicReference(null))

    suspend fun scanForID(id: ByteArray, timout: Long, callback: suspend () -> Unit) {
        var found = false
        val callbackWrap: suspend (ByteArray) -> Unit = { msg: ByteArray ->
            if ((!found) && id.zip(msg).all { (i, s) -> i == s }) {
                callback()
                found = true
            }
            yield()
        }
        try {
            withTimeout(timout) {
                handle.second.set(callbackWrap)
                startDevice()
                while (!found) {
                    yield()
                }
            }
        } // catch (_: TimeoutCancellationException) {}
        finally {
            stopDevice()
            // handlers.get().remove(callbackWrap)
            handle.second.set(null)
        }
    }

    suspend fun scanForMessages() {
        try {
            // handlers.get().add(callback.get())
            handle.first.set(callback.get())
            withTimeout(duration) {
                startDevice()
                while (true) yield()
            }
        } catch (_: TimeoutCancellationException) {} finally {
            stopDevice()
            // handlers.get().remove(callback.get())
            handle.first.set(null)
        }
    }

    private suspend fun startDevice() {
        if (scannerCount.getAndIncrement() == 0) {
            runner.set(
                GlobalScope.launch {
                    try {
                        device.beginReceiving(::scanForMessagesCallback)
                    } finally {
                        device.stopReceiving()
                    }
                })
        } else Unit
    }

    private fun stopDevice(): Unit =
        if (scannerCount.decrementAndGet() == 0)
            runner.getAndSet(null)?.cancel() ?: Unit // .set(device.stopReceiving()) ?: Unit
        else Unit

    private suspend fun scanForMessagesCallback(msg: ByteArray) {
        //   handlers.get().forEach { it(msg) }
        handle.first.get()?.invoke(msg)
        handle.second.get()?.invoke(msg)
    }

    fun setReceivedMessageCallback(f: suspend (ByteArray) -> Unit) = callback.set(f)

    // fun addReceivedMessageCallback(vararg f: suspend (ByteArray) -> Unit) =
    // handlers.get().addAll(f)
}
