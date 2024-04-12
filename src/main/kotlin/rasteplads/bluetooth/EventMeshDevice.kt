package rasteplads.bluetooth

import rasteplads.api.EventMesh

class EventMeshDevice(
    private val receiver: EventMeshReceiver,
    private val transmitter: EventMeshTransmitter,
) {

    private val receiveQueue = ArrayDeque<ByteArray>()

    init {
        receiver.handlers.add { message -> onMessageReceived(message) }
    }

    suspend fun transmit(ttl: UInt, id: ByteArray, message: ByteArray) {
        // Begin transmitting.
        require(id.size <= EventMesh.ID_MAX_SIZE)
        transmitter.transmit(message)

        // Begin listening for echos

        // Timeout if echos are not received.
        // Stop transmitting when echos are received.
        // Return when done.
    }

    fun getReceivedMessages(): Sequence<ByteArray> {
        return sequence { if (receiveQueue.isNotEmpty()) yield(receiveQueue.removeFirst()) }
    }

    private fun onMessageReceived(message: ByteArray) {}

    class Builder {
        private var receiver: EventMeshReceiver? = null
        private var transmitter: EventMeshTransmitter? = null

        fun withReceiver(receiver: EventMeshReceiver): Builder {
            this.receiver = receiver
            return this
        }

        fun withTransmitter(transmitter: EventMeshTransmitter): Builder {
            this.transmitter = transmitter
            return this
        }

        fun withDevice(device: TransportDevice): Builder {
            receiver = EventMeshReceiver(device)
            transmitter = EventMeshTransmitter(device)
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
