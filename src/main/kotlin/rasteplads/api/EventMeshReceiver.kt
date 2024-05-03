package rasteplads.api

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*

class EventMeshReceiver<Rx>(private val device: TransportDevice<Rx, *>) {
    var duration: Long = 10_000 // 10 sec //TODO: Default val
    // Not covered in code coverage, because it's an intermediate variable. Would need to use
    // reflection test it, if it should even be tested
    private val callback: AtomicReference<suspend (ByteArray) -> Unit> = AtomicReference {}
    private val handle:
        Pair<
            AtomicReference<suspend (ByteArray) -> Unit?>,
            AtomicReference<suspend (ByteArray) -> Boolean?>
        > =
        Pair(AtomicReference(null), AtomicReference(null))

    // BAD GUY
    fun scanForID(id: ByteArray, timeout: Long, callback: suspend () -> Unit) = runBlocking {
        val found = AtomicBoolean(false)
        val callbackWrap: suspend (ByteArray) -> Boolean = { msg: ByteArray ->
            var f = false
            if (id.zip(msg.drop(1)).all { (i, s) -> i == s }) {
                callback()
                f = true
            }
            yield()
            found.set(f)
            f
        }
        handle.second.set(callbackWrap)
        val key = startDevice(::scanForMessagesCallback)
        try {
            withTimeout(timeout) {
                //  handle.second.set(callbackWrap)
                while (!found.get()) yield()
            }
        } // catch (_: TimeoutCancellationException) {}
        finally {
            stopDevice(key)
            handle.second.set(null)
        }
    }

    // BAD GUY
    fun scanForMessages() = runBlocking {
        handle.first.set(callback.get())
        val key = startDevice(::scanForMessagesCallback)
        try {
            // handle.first.set(callback.get())
            withTimeout(duration) { while (true) yield() }
        } catch (_: TimeoutCancellationException) {} finally {
            stopDevice(key)
            handle.first.set(null)
        }
    }

    private fun startDevice(callback: suspend (ByteArray) -> Unit): Rx =
        device.beginReceiving(callback)

    private fun stopDevice(stop: Rx) {
        device.stopReceiving(stop)
    }

    private suspend fun scanForMessagesCallback(msg: ByteArray) {
        if (handle.second.get()?.invoke(msg) != true) handle.first.get()?.invoke(msg)
    }

    fun setReceivedMessageCallback(f: suspend (ByteArray) -> Unit) = callback.set(f)
}
