package rasteplads.api

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlinx.coroutines.*

// TODO: Handling of timeouts should be reconsidered (or maybe implement a counter of sorts)

class EventMeshReceiver(private val device: TransportDevice) {
    var duration: Long = 10_000 // 10 sec //TODO: Default val
    private val scannerCount: AtomicInteger = AtomicInteger(0)
    private var runner: AtomicReference<Job?> = AtomicReference(null)
    // Not covered in code coverage, because it's an intermediate variable. Would need to use
    // reflection test it
    private val callback: AtomicReference<suspend (ByteArray) -> Unit> = AtomicReference {}
    private val handle:
        Pair<
            AtomicReference<suspend (ByteArray) -> Unit?>,
            AtomicReference<suspend (ByteArray) -> Unit?>> =
        Pair(AtomicReference(null), AtomicReference(null))

    fun scanForID(id: ByteArray, timout: Long, callback: suspend () -> Unit) = runBlocking {
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
                while (!found) yield()
            }
        } // catch (_: TimeoutCancellationException) {}
        finally {
            stopDevice()
            handle.second.set(null)
        }
    }

    fun scanForMessages() = runBlocking {
        try {
            handle.first.set(callback.get())
            withTimeout(duration) {
                startDevice()
                while (true) yield()
            }
        } catch (_: TimeoutCancellationException) {} finally {
            stopDevice()
            handle.first.set(null)
        }
    }

    private fun startDevice() {
        if (scannerCount.getAndIncrement() <= 0) {
            runner.set(
                GlobalScope.launch {
                    try {
                        device.beginReceiving(::scanForMessagesCallback)
                    } finally {
                        device.stopReceiving()
                    }
                })
        }
    }

    private fun stopDevice() {
        if (scannerCount.updateAndGet { old -> max(old - 1, 0) } == 0) {
            device.stopReceiving()
            runner.getAndSet(null)?.cancel() // .set(device.stopReceiving()) ?: Unit
        }
    }

    private suspend fun scanForMessagesCallback(msg: ByteArray) {
        handle.first.get()?.invoke(msg)
        handle.second.get()?.invoke(msg)
    }

    fun setReceivedMessageCallback(f: suspend (ByteArray) -> Unit) = callback.set(f)
}
