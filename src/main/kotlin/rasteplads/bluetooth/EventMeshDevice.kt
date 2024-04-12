package rasteplads.bluetooth

class EventMeshDevice<T>(
    private val receiver: EventMeshReceiver<T>,
    private val transmitter: EventMeshTransmitter<T>,
) {

    private val receiveQueue = ArrayDeque<Message<T>>()
    init {
        receiver.handlers.add {message -> onMessageReceived(message)}
    }

    suspend fun transmit(message: Message<T>){
        // Begin transmitting.
        transmitter.transmit(message)

        // Begin listening for echos

        // Timeout if echos are not received.
        // Stop transmitting when echos are received.
        // Return when done.
    }

    fun getReceivedMessages(): Sequence<Message<T>>{
        return sequence {
            if (receiveQueue.isNotEmpty())
                yield(receiveQueue.removeFirst())
        }
    }

    private fun onMessageReceived(message: Message<T>){

    }

    class Builder<T> {
        private var receiver: EventMeshReceiver<T>? = null
        private var transmitter: EventMeshTransmitter<T>? = null

        fun withReceiver(receiver: EventMeshReceiver<T>): Builder<T> {
            this.receiver = receiver
            return this
        }
        fun withTransmitter(transmitter: EventMeshTransmitter<T>): Builder<T> {
            this.transmitter = transmitter
            return this
        }
        fun build(): EventMeshDevice<T> {
            // TODO: Construct tx and rx if none are provided.
            //val transmitter = this.transmitter ?: EventMeshTransmitter<T>()
            if (transmitter == null)
                throw NotImplementedError("A transmitter must be specified when using the EventMeshDevice.Builder.")
            if (receiver == null)
                throw NotImplementedError("A receiver must be specified when using the EventMeshDevice.Builder.")

            return EventMeshDevice<T>(receiver!!, transmitter!!)
        }
    }
}