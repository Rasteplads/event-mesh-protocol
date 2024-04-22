package rasteplads.util

import kotlin.test.*
import org.junit.jupiter.api.Test

class EitherTest {
    @Test
    fun `either test`() {
        var e = Either.left<Int, UInt>(8)
        assert(e.isLeft())
        assertFalse(e.isRight())
        assertNotNull(e.getLeft())
        assertNull(e.getRight())
        assertEquals(8, e.getLeft()!!)

        e = Either.right(8u)
        assert(e.isRight())
        assertFalse(e.isLeft())
        assertNotNull(e.getRight())
        assertNull(e.getLeft())
        assertEquals(8u, e.getRight()!!)
    }
}
