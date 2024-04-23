package rasteplads.api

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlinx.coroutines.*

class EventMeshReceiver(private val device: TransportDevice) {
    var duration: Long = 10_000 // 10 sec //TODO: Default val
    private val scannerCount: AtomicInteger = AtomicInteger(0)
    private var runner: AtomicReference<Job?> = AtomicReference(null)
    // Not covered in code coverage, because it's an intermediate variable. Would need to use
    // reflection test it, if it should even be tested
    private val callback: AtomicReference<suspend (ByteArray) -> Unit> = AtomicReference {}
    private val handle:
        Pair<
            AtomicReference<suspend (ByteArray) -> Unit?>,
            AtomicReference<suspend (ByteArray) -> Boolean?>
        > =
        Pair(AtomicReference(null), AtomicReference(null))

    fun scanForID(id: ByteArray, timeout: Long, callback: suspend () -> Unit) = runBlocking {
        val found = AtomicBoolean(false)
        val callbackWrap: suspend (ByteArray) -> Boolean = { msg: ByteArray ->
            if (id.zip(msg).all { (i, s) -> i == s }) {
                callback()
                found.set(true)
            }
            yield()
            found.get()
        }
        try {
            withTimeout(timeout) {
                handle.second.set(callbackWrap)
                startDevice()
                while (!found.get()) yield()
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
        if (scannerCount.getAndIncrement() <= 0)
            runner.set(GlobalScope.launch { device.beginReceiving(::scanForMessagesCallback) })
    }

    private fun stopDevice() = runBlocking {
        if (scannerCount.updateAndGet { old -> max(old - 1, 0) } == 0) {
            device.stopReceiving()
            runner.getAndSet(null)?.join() // .set(device.stopReceiving()) ?: Unit
        }
    }

    private suspend fun scanForMessagesCallback(msg: ByteArray) {
        if (handle.second.get()?.invoke(msg) != true) handle.first.get()?.invoke(msg)
    }

    fun setReceivedMessageCallback(f: suspend (ByteArray) -> Unit) = callback.set(f)
}
