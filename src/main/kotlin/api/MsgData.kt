package org.example.api

import kotlinx.serialization.Serializable

typealias Bytes = ByteArray

interface tesss {

}

const val MAX_SIZE_MSGDATA = 29u


//interface MsgData<D> : Into<Bytes>, From<Bytes, D> {
interface MsgData<D> : IntoFrom<Bytes, D> {

    /// The number of bytes being broadcast
    val size: UInt

    val test: (value: Bytes) -> D
}

interface From<in T, out O> {
    fun T.from(): O

    fun <T> createFrom(input: T): O
}


interface Into<out T> {
    fun into(): T
}

interface IntoFrom<T, out O> {
    fun into(): T
    fun from(value: T): O
}

interface TT {
    val g: Byte;
    companion object benis;
}

interface FromInto<T, O> {
    val into: (T) -> O
    val from: (O) -> T
}

interface FromInto1<T, O> {
    fun into(): O
    fun O.fromE(): T
    fun from(value: O): T
}

@Serializable
class Temp(val i: Int) {
    companion object : FromInto<ByteArray, Temp> {
        override val into: (ByteArray) -> Temp
            get() = { b -> Temp(b.toInt())}
        override val from: (Temp) -> ByteArray
            get() = TODO("Not yet implemented")
    }

}


private fun Bytes.toInt(): Int = this.take(4).fold(0, {acc, b -> (acc shl 8) or (b.toInt())})
private fun Bytes.toUInt(): UInt = this.take(4).fold(0u, {acc, b -> (acc shl 8) or (b.toUInt())})
private fun Bytes.toLong(): Long = this.take(8).fold(0, {acc, b -> (acc shl 8) or (b.toLong())})



class Hej(override val g: Byte) : From<ByteArray, Hej>, TT {

    fun test() {
        var v = byteArrayOf(1,2,4,5,8);
        v.from();
    }
    override fun ByteArray.from(): Hej {

        TODO("Not yet implemented")
    }

    override fun <T> createFrom(input: T): Hej {
        TODO("Not yet implemented")
    }

}

interface From1<in T, out O> {
    fun T.from(): O

    companion object {
        // Static method accessible outside the implementing class

        inline fun <reified T, O> createFrom(input: T): O {
            // Implementation logic here
            TODO()
        }
    }

    interface Factory<in A, out B> {
        // Abstract method to be implemented in the companion object
        fun createFrom(input: A): B
    }
}

interface Comp {
    companion object {}
    val companion: Comp.Companion;
}

class MyClass {
    companion object : From1.Factory<String, Int> {
        override fun createFrom(input: String): Int {
            // Implementation of createFrom method
            println("yes")
            return 9
        }
    }
}

interface From2 {

}

interface IntoFrom1<I, O> {
    fun into(): O
    companion object : From2 {

    }
}
