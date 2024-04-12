package rasteplads.bluetooth

abstract class Message<T>(protected val data: T){
    abstract fun toByteArray(): ByteArray
}

class StringMessage(data: String) : Message<String>(data){
    override fun toString(): String {
        return this.data
    }

    override fun toByteArray(): ByteArray {
        return this.data.toByteArray()
    }
}

abstract class MessageDecoder<T>{
    abstract fun decode(byteArray: ByteArray): Message<T>?
}

class StringMessageDecoder: MessageDecoder<String>() {
    override fun decode(byteArray: ByteArray): Message<String>? {
        // Try to decode, fail if impossible.
        return StringMessage(byteArray.toString())
    }
}