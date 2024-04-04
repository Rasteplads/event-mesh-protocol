package org.example.api

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule


inline fun <reified T> encodeToByteArray(value: T): ByteArray {
    val result = ByteArrayWriter()
        val encoder = ByteArrayEncoder()
        encoder.encodeSerializableValue(encoder.serializersModule.serializer(), value)
        return result.result()
}

fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@Serializable
data class Project(val i: Int, val b: Byte, val s: String)


class ListEncoder : AbstractEncoder() {
    var list = mutableListOf<Any>()
    override val serializersModule: SerializersModule = EmptySerializersModule()


    override fun encodeValue(value: Any) {
        list.add(value)
    }
}


class ByteArrayEncoder : Encoder {
    override val serializersModule: SerializersModule
        get() = SerializersModule {  }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        TODO("Not yet implemented")

    }

    override fun encodeBoolean(value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun encodeByte(value: Byte) {
        TODO("Not yet implemented")
    }

    override fun encodeChar(value: Char) {
        TODO("Not yet implemented")
    }

    override fun encodeDouble(value: Double) {
        TODO("Not yet implemented")
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeFloat(value: Float) {
        TODO("Not yet implemented")
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        TODO("Not yet implemented")
    }

    override fun encodeInt(value: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeLong(value: Long) {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun encodeNull() {
        TODO("Not yet implemented")
    }

    override fun encodeShort(value: Short) {
        TODO("Not yet implemented")
    }

    override fun encodeString(value: String) {
        TODO("Not yet implemented")
    }

}

/*
object BytesParser : BinaryFormat {
    override val serializersModule: SerializersModule
        get() = TODO("Not yet implemented")

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        return decodeFromByteArray<T>(bytes);
        //deserializer.deserialize()
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {

        serializer.serialize(Encoder, value)
    }
}
 */

@PublishedApi
internal interface Writer<T> {
    fun writeLong(value: Long)
    fun writeInt(value: Int)
    fun writeShort(value: Short)
    fun writeByte(byte: Byte)
    fun writeChar(char: Char)
    fun writeString(text: String)
    fun result(): T
}

class ByteArrayWriter : Writer<ByteArray> {
    private var array: ByteArray = byteArrayOf();
    private var size = 0

    override fun writeLong(value: Long) {
        ensureAdditionalCapacity(8)
        var i = 8;

        while (i >= 0) {
            array[size++] = ((value shr (8 * (i--))) and 0xff).toByte()
            i--;
        }
        // Can be hand-rolled, but requires a lot of code and corner-cases handling

        //write(value.toString())
    }

    override fun writeInt(value: Int) {
        ensureAdditionalCapacity(4)
        var i = 4;

        while (i >= 0) {
            array[size++] = ((value shr (8 * (i--))) and 0xff).toByte()
            i--;
        }
        // Can be hand-rolled, but requires a lot of code and corner-cases handling

        //write(value.toString())
    }

    override fun writeShort(value: Short) {
        ensureAdditionalCapacity(2)
        array[size++] = (value.toInt() and 0xff).toByte();
        array[size++] = ((value.toInt() shr 8) and 0xff).toByte();
    }

    override fun writeByte(byte: Byte) {
        ensureAdditionalCapacity(1)
        array[size++] = byte
    }

    override fun writeChar(char: Char) {
        ensureAdditionalCapacity(1)
        array[size++] = char.code.toByte()
    }

    override fun writeString(text: String) {
        val length = text.length
        if (length == 0) return
        ensureAdditionalCapacity(length)
        text.toByteArray().forEach { b -> array[size++] = b };
        //text.toCharArray(array, size, 0, length)
        //size += length
    }


    override fun result(): ByteArray {
        return array;
    }

    private fun ensureAdditionalCapacity(expected: Int) {
        ensureTotalCapacity(size, expected)
    }

    // Old size is passed and returned separately to avoid excessive [size] field read
    private fun ensureTotalCapacity(oldSize: Int, additional: Int): Int {
        val newSize = oldSize + additional
        if (array.size <= newSize) {
            array = array.copyOf(newSize.coerceAtLeast(oldSize * 2))
        }
        return oldSize
    }
}



private fun Bytes.toInt(): Int = this.take(4).fold(0, {acc, b -> (acc shl 8) or (b.toInt())})
private fun Bytes.toUInt(): UInt = this.take(4).fold(0u, {acc, b -> (acc shl 8) or (b.toUInt())})
private fun Bytes.toLong(): Long = this.take(8).fold(0, {acc, b -> (acc shl 8) or (b.toLong())})


class LOL : BinaryFormat {
    override val serializersModule: SerializersModule
        get() = TODO("Not yet implemented")

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        TODO("Not yet implemented")
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        TODO("Not yet implemented")
    }

}

