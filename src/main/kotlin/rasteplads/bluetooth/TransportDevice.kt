package rasteplads.bluetooth

import kotlinx.coroutines.channels.Channel

interface TransportDevice {
    var messageBuffer: MessageBuffer<ByteArray>
    val receiveChannel: Channel<ByteArray>
    val transmitChannel: Channel<ByteArray>
    val transmissionInterval: Long

    fun beginTransmitting(message: ByteArray)

    fun stopTransmitting()

    fun beginReceiving(callback: suspend (ByteArray) -> Unit)

    fun stopReceiving()
}
