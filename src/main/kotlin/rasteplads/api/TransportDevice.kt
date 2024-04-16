package rasteplads.api

import kotlinx.coroutines.channels.Channel

interface TransportDevice {
    val receiveChannel: Channel<ByteArray>
    val transmitChannel: Channel<ByteArray>
    val transmissionInterval: Long

    suspend fun beginTransmitting(message: ByteArray)

    fun stopTransmitting()

    suspend fun beginReceiving(callback: suspend (ByteArray) -> Unit)

    fun stopReceiving()
}
