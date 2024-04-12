package rasteplads.api.eventmesh_test

import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertFails
import org.junit.jupiter.api.Test
import rasteplads.api.EventMesh
import rasteplads.api.eventmesh_test.BuilderTest.Companion.correct

class ExecutingTest {

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
