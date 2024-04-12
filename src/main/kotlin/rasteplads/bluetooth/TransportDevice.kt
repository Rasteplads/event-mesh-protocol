package rasteplads.bluetooth

import kotlinx.coroutines.channels.Channel

interface TransportDevice {
    var messageBuffer: MessageBuffer<ByteArray>
    val receiveChannel: Channel<ByteArray>
    val transmitChannel: Channel<ByteArray>

    fun beginTransmitting(message: ByteArray)

    fun stopTransmitting(message: ByteArray)

    fun beginReceiving()

    fun stopReceiving()
}
