package rasteplads.api.eventmesh_test

import org.junit.jupiter.api.Test
import rasteplads.api.EventMesh
import rasteplads.util.toByteArray

class ExecutingTest {

    private fun correct() =
        EventMesh.builder<Byte, Int>()
            .setDataConstant(0)
            .setIDGenerator { 10 }
            .setHandleMessage { _, _ -> }
            .setIntoIDFunction { _ -> 9 }
            .setIntoDataFunction { _ -> 0 }
            .setFromIDFunction { _ -> byteArrayOf(0, 1, 2, 3) }
            .setFromDataFunction { it.toByteArray() }

    @Test
    fun returnArraysTooLarge() {
        // TODO:
    }
}
