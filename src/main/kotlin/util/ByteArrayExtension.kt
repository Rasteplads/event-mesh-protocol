package org.example.util

import kotlinx.serialization.internal.*
import java.nio.ByteBuffer

//import kotlin.experimental.and
//import kotlinx.serialization.internal.ByteArraySerializer.collectionIterator


fun ByteArray.toByte(): Byte = this.firstOrNull() ?: 0
fun ByteArray.toShort(): Short = ByteBuffer.wrap(this).getShort(0)//this.take(2).fold(0, {acc, b -> (acc shl 8) or (b.toInt())}).toShort()
fun ByteArray.toInt(): Int = ByteBuffer.wrap(this).getInt()//this.take(4).fold(0, {acc, b -> (acc shl 8) or (b.toInt())})
fun ByteArray.toLong(): Long = ByteBuffer.wrap(this).getLong()//this.take(8).fold(0, {acc, b -> (acc shl 8) or (b.toLong())})
//fun ByteArray.toUInt(): UInt = this.toInt().toUInt()


/*
fun UByteArray.toUByte(): UByte = this.first()
fun UByteArray.toByte(): Byte = this.toUByte().toByte()
fun UByteArray.toUShort(): UShort = this.take(2).fold(0u, {acc, b -> ((acc + 0xffu).toUShort() and (b.toUShort())) })
fun UByteArray.toShort(): Short = this.take(2).fold(0u, {acc, b -> ((acc + 0xffu) and (b.toUInt())) }).toShort()
fun UByteArray.toUInt(): UInt = this.take(4).fold(0u, {acc, b -> (acc shl 8) or (b.toUInt())})
fun UByteArray.toInt(): Int = this.toUInt().toInt()
fun UByteArray.toULong(): ULong = this.take(8).fold(0u, {acc, b -> (acc shl 8) or (b.toULong())})
fun UByteArray.toLong(): Long = this.toULong().toLong()

fun numberToByteArray (data: Number, size: Int = 8) : ByteArray =
    ByteArray (size) {i -> (data.toLong() shr (i*8)).toByte()}

fun numberToUByteArray (data: ULong, size: Int = 8) : UByteArray =
    UByteArray (size) {i -> (data shr (i*8)).toUByte()}
*/
fun Byte.toByteArray(): ByteArray = byteArrayOf(this)
//fun Byte.toUByteArray(): UByteArray = ubyteArrayOf(this.toUByte())
fun Short.toByteArray(): ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES).putShort(this).array()//numberToByteArray(this, 2) //ByteBuffer.allocateDirect(2).putShort(this).array()//byteArrayOf(((this-0xff) and 0xff).toByte(), (this-0xff).toByte())
//fun Short.toUByteArray(): UByteArray = numberToUByteArray(this.toULong(), 2)
   // fun Short.toUByteArray(): UByteArray {
fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array() //arrayOf(24, 16, 8, 0).map({i -> ((this shr i) and (0xff)).toByte()}).toByteArray()
//fun Int.toUByteArray(): UByteArray = this.toByteArray().map { it.toUByte() }.toUByteArray()
fun Long.toByteArray(): ByteArray = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()//arrayOf(56, 48, 40, 32, 24, 16, 8, 0).map({i -> ((this shr i) and (0xff)).toByte()}).toByteArray()
//fun Long.toUByteArray(): UByteArray = this.toByteArray().map { it.toUByte() }.toUByteArray()

//fun UByte.toByteArray(): ByteArray = byteArrayOf(this.toByte())
//fun UByte.toUByteArray(): UByteArray = ubyteArrayOf(this)
//fun UShort.toByteArray(): ByteArray = byteArrayOf(((this - 0xffu) and 0xffu).toByte(), (this and 0xffu).toByte())
//fun UShort.toUByteArray(): UByteArray = this.toByteArray().map { it.toUByte() }.toUByteArray()
//fun UInt.toByteArray(): ByteArray = arrayOf(24, 16, 8, 0).map({i -> ((this shr i) and (0xffu)).toByte()}).toByteArray()
//fun UInt.toUByteArray(): UByteArray = arrayOf(24, 16, 8, 0).map({i -> ((this shr i) and (0xffu)).toUByte()}).toUByteArray()
//fun ULong.toByteArray(): ByteArray = arrayOf(56, 48, 40, 32, 24, 16, 8, 0).map({i -> ((this shr i) and (0xffu)).toByte()}).toByteArray()
//fun ULong.toUByteArray(): UByteArray = this.toByteArray().map { it.toUByte() }.toUByteArray()
