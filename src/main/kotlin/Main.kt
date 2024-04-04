package org.example

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.encodeToByteArray
import org.example.api.*
import kotlin.reflect.KClass

//import kotlinx.serialization.cbor.Cbor.Default.encodeToByteArray

@Serializable
class TT(val sllllllllll: Int, val t: Byte, val p: Long) {
    override fun toString(): String = "$sllllllllll, $t, $p";
};

fun <T: FromInto<ByteArray, U>, U> testt(value: T) {
    var v = byteArrayOf(1,2,4,5,8);
    println(value.into(v))
}

fun <T, U> testtt(value: T) where T : KSerializer<U> {
    var v = byteArrayOf(1,2,4,5,8);
}


fun <T> testttt(value: T) where T : FromInto1<T, ByteArray> {
    var v = byteArrayOf(1,2,4,5,8);
    var h: T = value.from(v)

}



fun main() {

    val p = Project(6, 1, "BITHC");
    println(encodeToList(p));

    val t = TT(5, 3, 1);
    //ByteArraySerializer().serialize(ByteArraySerializer(), t);
    //encodeToByteArray(ByteArraySerializer(), t);
    println(t);
    var v = byteArrayOf(1,2,4,5,8);
    //var h: Hej = v.from();
    val b = Cbor.encodeToByteArray(t);
    println(b.map { b -> b.toInt() });
    //println(Cbor.decodeFromByteArray<TT>(b));
    //encodeToByteArray(t);
    //println(Json.encodeToString(t).toByteArray());
    //println(encodeToByteArray(t));
    /*
    val t = T()
    val test = Test();
    val b = byteArrayOf(-1, 1)
   // val te: Test = b.hej({b -> Test.from(b));
    val test2 = b.into()
   // println(getem(t));
   // println(getem(t));
   // println(Json.encodeToString(t));
    println("Hello World!")
    //println(sizeOf<Int>());
     */
}

fun Bytes.into(): Test {
    println("JAAA")
    TODO("Not yet implemented")
}

suspend inline fun <reified T: MsgData<T>> on_message(f: (T) -> Unit): Unit {
    // Get bytes from BT
    val bytes = BTTEST();
    //CHECK MESSAGE CACHE, IF GO, FUNC and then save
    //val v: T = bytes.into<T>();

}

fun BTTEST(): Bytes = byteArrayOf(-1, 1);

class Test : MsgData<Test> {

    override val size: UInt
        get() = TODO("Not yet implemented")
    override val test: (value: Bytes) -> Test
        get() = TODO("Not yet implemented")

    override fun into(): Bytes {
        TODO("Not yet implemented")
    }

    override fun from(value: Bytes): Test {
        TODO("Not yet implemented")
    }


}

/*

class Test : MsgData<Test> {

    override val size = 4u;
    override val test: (value: Bytes) -> Test
        get() {
            TODO()
        }


    override fun into(): Bytes {
        return byteArrayOf()
    }
}
*/
