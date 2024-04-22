package rasteplads.util

import java.nio.ByteBuffer

fun ByteArray.toByte(): Byte = ByteBuffer.wrap(this)[0]

fun ByteArray.toUByte(): UByte = this.toByte().toUByte()

fun ByteArray.toShort(): Short = ByteBuffer.wrap(this).getShort()

fun ByteArray.toUShort(): UShort = this.toShort().toUShort()

fun ByteArray.toInt(): Int = ByteBuffer.wrap(this).getInt()

fun ByteArray.toUInt(): UInt = this.toInt().toUInt()

fun ByteArray.toLong(): Long = ByteBuffer.wrap(this).getLong()

fun ByteArray.toULong(): ULong = this.toLong().toULong()

fun Byte.toByteArray(): ByteArray = byteArrayOf(this)

fun UByte.toByteArray(): ByteArray = this.toByte().toByteArray()

fun Short.toByteArray(): ByteArray = ByteBuffer.allocate(Short.SIZE_BYTES).putShort(this).array()

fun UShort.toByteArray(): ByteArray = this.toShort().toByteArray()

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()

fun UInt.toByteArray(): ByteArray = this.toInt().toByteArray()

fun Long.toByteArray(): ByteArray = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()

fun ULong.toByteArray(): ByteArray = this.toLong().toByteArray()

operator fun Byte.plus(other: ByteArray): ByteArray = byteArrayOf(this) + other

fun ByteArray.split(i: Int): Pair<ByteArray, ByteArray> =
    Pair(this.sliceArray(0 until i), this.sliceArray(i until this.size))
