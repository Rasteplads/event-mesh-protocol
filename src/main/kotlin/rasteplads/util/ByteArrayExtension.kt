package rasteplads.util

import java.nio.ByteBuffer

fun ByteArray.toByte(): Byte = ByteBuffer.wrap(this)[0]

fun ByteArray.toShort(): Short = ByteBuffer.wrap(this).getShort()

fun ByteArray.toInt(): Int = ByteBuffer.wrap(this).getInt()

fun ByteArray.toLong(): Long = ByteBuffer.wrap(this).getLong()

fun Byte.toByteArray(): ByteArray = byteArrayOf(this)

fun Short.toByteArray(): ByteArray = ByteBuffer.allocate(Short.SIZE_BYTES).putShort(this).array()

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()

fun Long.toByteArray(): ByteArray = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()
