package rasteplads.api

import java.time.Duration
import kotlinx.coroutines.*
import rasteplads.api.EventMesh.Companion.ID_MAX_SIZE

operator fun Byte.plus(other: ByteArray): ByteArray = byteArrayOf(this) + other

class EventMeshDevice(
    private val receiver: EventMeshReceiver,
    private val transmitter: EventMeshTransmitter,
    txTimeout: Duration? = null,
    rxDuration: Duration? = null,
    private val echo: (() -> Unit)? = null
) {

    init {
        txTimeout?.let { transmitter.transmitTimeout = it.toMillis() }
        rxDuration?.let { receiver.duration = it.toMillis() }
    }

    fun startTransmitting(ttl: Byte, id: ByteArray, message: ByteArray) = runBlocking {
        check(id.size <= ID_MAX_SIZE) { "ID too big" }
        val combinedMsg = ttl + id + message
        val tx = launch { transmitter.transmit(combinedMsg) }

        try {
            // withTimeout(transmitter.transmitTimeout) {
            receiver.scanForID(id, transmitter.transmitTimeout) { tx.cancel() }
            // }
        } catch (e: TimeoutCancellationException) {
            echo?.invoke()
        } finally {}
    }

    fun startReceiving() = receiver.scanForMessages()

    class Builder {
        private var receiver: EventMeshReceiver? = null
        private var transmitter: EventMeshTransmitter? = null
        private var tTimeout: Duration? = null
        private var rDuration: Duration? = null
        private var echo: (() -> Unit)? = null

        fun withReceiver(receiver: EventMeshReceiver): Builder {
            this.receiver = receiver
            return this
        }

        fun withTransmitter(transmitter: EventMeshTransmitter): Builder {
            this.transmitter = transmitter
            return this
        }

        fun withTransmitTimeout(d: Duration): Builder {
            // transmitter!!.transmitTimeout = d.toMillis()
            tTimeout = d
            return this
        }

        fun withReceiveDuration(d: Duration): Builder {
            rDuration = d
            return this
        }

        fun withEchoCallback(e: (() -> Unit)?): Builder {
            echo = e
            return this
        }

        fun build(): EventMeshDevice {
            check(transmitter != null) {
                "A transmitter must be specified when using the EventMeshDevice.Builder."
            }
            check(receiver != null) {
                "A receiver must be specified when using the EventMeshDevice.Builder."
            }
            // TODO: Construct tx and rx if none are provided.
            // val transmitter = this.transmitter ?: EventMeshTransmitter<T>()

            return EventMeshDevice(receiver!!, transmitter!!, echo = echo)
        }
    }
}
