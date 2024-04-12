package rasteplads.api.eventmesh_test

import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertFails
import org.junit.jupiter.api.Test
import rasteplads.api.EventMesh
import rasteplads.util.toByteArray

class ExecutingTest {

    private fun correct() =
        EventMesh.builder<Byte, Int>()
            .setDataConstant(0)
            .setIDGenerator { 10 }
            .setMessageCallback { _, _ -> }
            .setIDDecodeFunction { _ -> 9 }
            .setDataDecodeFunction { _ -> 0 }
            .setIDEncodeFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setDataEncodeFunction { it.toByteArray() }

    private inline fun <reified C, reified R> getValueFromClass(target: C, field: String): R =
        C::class.members.find { m -> m.name == field }!!.apply { isAccessible = true }.call(target)
            as R

    @Test
    fun returnArraysTooLarge() {
        val f = correct().build()
        val runFunc: (String, ByteArray) -> Unit = { name, b ->
            EventMesh::class
                .members
                .find { m -> m.name == name }!!
                .apply { isAccessible = true }
                .call(f, b)
        }
        val scan = "scanningCallback"
        assertFails { runFunc(scan, byteArrayOf(1)) }
        assertFails { runFunc(scan, byteArrayOf(1, 2)) }
        assertFails { runFunc(scan, byteArrayOf(1, 2, 3)) }
        assertFails { runFunc(scan, byteArrayOf(1, 2, 3, 4)) }

        // TODO: more
    }
}
