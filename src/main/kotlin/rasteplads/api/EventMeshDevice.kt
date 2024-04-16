package rasteplads.api

import java.time.Duration
import kotlinx.coroutines.*
import rasteplads.api.EventMesh.Companion.ID_MAX_SIZE

private operator fun Byte.plus(other: ByteArray): ByteArray = byteArrayOf(this) + other

class EventMeshDevice(
    private val receiver: EventMeshReceiver,
    private val transmitter: EventMeshTransmitter,
    txTimeout: Duration? = null,
    rxDuration: Duration? = null,
) {

    init {
        txTimeout?.let { transmitter.transmitTimeout = it.toMillis() }
        rxDuration?.let { receiver.duration = it.toMillis() }
    }

    fun startTransmitting(ttl: Byte, id: ByteArray, message: ByteArray) = runBlocking {
        check(id.size <= ID_MAX_SIZE) { "ID too big" }
        // Begin transmitting.
        // If null, no echo
        val combinedMsg = ttl + id + message
        val tx = launch { transmitter.transmit(combinedMsg) }

        // TODO: PLS
        /*withTimeoutOrNull(transmitter.transmitTimeout) {
            receiver.scanForID(id) { tx.cancelAndJoin() }
        }
            ?: Unit*/
    }

    suspend fun startReceiving() = receiver.scanForMessages()

    class Builder {
        private var receiver: EventMeshReceiver? = null
        private var transmitter: EventMeshTransmitter? = null
        private var tTimeout: Duration? = null
        private var rDuration: Duration? = null

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

        fun build(): EventMeshDevice {
            check(transmitter != null) {
                "A transmitter must be specified when using the EventMeshDevice.Builder."
            }
            check(receiver != null) {
                "A receiver must be specified when using the EventMeshDevice.Builder."
            }
            // TODO: Construct tx and rx if none are provided.
            // val transmitter = this.transmitter ?: EventMeshTransmitter<T>()

            return EventMeshDevice(receiver!!, transmitter!!)
        }
    }
}
