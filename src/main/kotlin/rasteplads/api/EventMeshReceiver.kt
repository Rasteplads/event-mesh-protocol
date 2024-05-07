package rasteplads.api

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlinx.coroutines.*

open class EventMeshReceiver<TRx>(private val device: TransportDevice<TRx, *>) {
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

    fun scanForID(id: ByteArray, timeout: Long, callback: suspend () -> Unit) = runBlocking {
        var cb: TRx? = null
        val found = AtomicBoolean(false)
        val callbackWrap: suspend (ByteArray) -> Boolean = { msg: ByteArray ->
            if (id.zip(msg.drop(1)).all { (i, s) -> i == s }) {
                callback()
                found.set(true)
            }
            yield()
            found.get()
        }
        try {
            withTimeout(timeout) {
                handle.second.set(callbackWrap)
                cb = device.beginReceiving(::scanForMessagesCallback)
                while (!found.get()) yield()
            }
        } // catch (_: TimeoutCancellationException) {}
        finally {
            if (cb != null){
                device.stopReceiving(cb!!)
            }
            handle.second.set(null)
        }
    }

    fun scanForMessages() = runBlocking {
        var cb: TRx? = null
        try {
            handle.first.set(callback.get())
            withTimeout(duration) {
                cb = device.beginReceiving(::scanForMessagesCallback)
                while (true) yield()
            }
        } catch (_: TimeoutCancellationException) {} finally {
            if (cb != null)
                device.stopReceiving(cb!!)
            handle.first.set(null)
        }
    }

    private suspend fun scanForMessagesCallback(msg: ByteArray) {
        if (handle.second.get()?.invoke(msg) != true) handle.first.get()?.invoke(msg)
    }

    fun setReceivedMessageCallback(f: suspend (ByteArray) -> Unit) = callback.set(f)
}
