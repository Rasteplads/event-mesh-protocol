package rasteplads.messageCacheTest

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test
import rasteplads.messageCache.MessageCache

class MessageCacheTest {
    @Test
    fun testOutdatedStringMessage() {
        val cache = MessageCache<String>(60)
        cache.cacheMessage("first")
        Thread.sleep(60000L)
        cache.cacheMessage("second")

        assertFalse(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun testLifeTimeMessage() {
        val cache1 = MessageCache<String>(30)
        val cache2 = MessageCache<String>(60)
        cache1.cacheMessage("first")
        cache2.cacheMessage("first2")
        Thread.sleep(30000L)
        cache1.cacheMessage("second")
        cache2.cacheMessage("second2")

        assertFalse(cache1.containsMessage("first"))
        assert(cache1.containsMessage("second"))
        assertEquals(cache1.getSize(), 1)
        assert(cache2.containsMessage("first2"))
        assert(cache2.containsMessage("second2"))
        assertEquals(cache2.getSize(), 2)
    }

    @Test
    fun testChangeCacheTime() {
        val cache = MessageCache<String>(30)
        cache.cacheMessage("first")
        cache.changeCacheTime(60)
        cache.cacheMessage("second")
        Thread.sleep(30000L)
        cache.cacheMessage("third")

        assertFalse(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assert(cache.containsMessage("third"))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun testNoOutdatedStringMessage() {
        val cache = MessageCache<String>(60)
        cache.cacheMessage("first")
        cache.cacheMessage("second")

        assert(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun testDuplicateStringKey() {
        val cache = MessageCache<String>(60)
        cache.cacheMessage("first")
        cache.cacheMessage("first")

        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun testDuplicateIntKey() {
        val cache = MessageCache<Int>(60)
        cache.cacheMessage(1)
        cache.cacheMessage(1)

        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun testNoOutdatedIntMessage() {
        val cache = MessageCache<Int>(60)
        cache.cacheMessage(1)
        cache.cacheMessage(2)

        assert(cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun testOutdatedIntMessage() {
        val cache = MessageCache<Int>(60)
        cache.cacheMessage(1)
        Thread.sleep(60000L)
        cache.cacheMessage(2)

        assertFalse(cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assertEquals(cache.getSize(), 1)
    }
}
