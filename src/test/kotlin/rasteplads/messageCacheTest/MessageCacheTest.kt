package rasteplads.messageCacheTest

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.example.cache.MessageCache
import org.junit.jupiter.api.Test

class MessageCacheTest {
    @Test
    fun testOutdatedStringMessage() {
        val cache = MessageCache<String>(1)
        cache.cacheMessage("first")
        Thread.sleep(60000L)
        cache.cacheMessage("second")

        assertFalse(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun testNoOutdatedStringMessage() {
        val cache = MessageCache<String>(1)
        cache.cacheMessage("first")
        cache.cacheMessage("second")

        assert(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun testDuplicateStringKey() {
        val cache = MessageCache<String>(1)
        cache.cacheMessage("first")
        cache.cacheMessage("first")

        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun testDuplicateIntKey() {
        val cache = MessageCache<Int>(1)
        cache.cacheMessage(1)
        cache.cacheMessage(1)

        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun testNoOutdatedIntMessage() {
        val cache = MessageCache<Int>(1)
        cache.cacheMessage(1)
        cache.cacheMessage(2)

        assert(cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun testOutdatedIntMessage() {
        val cache = MessageCache<Int>(1)
        cache.cacheMessage(1)
        Thread.sleep(60000L)
        cache.cacheMessage(2)

        assertFalse(cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assertEquals(cache.getSize(), 1)
    }
}
