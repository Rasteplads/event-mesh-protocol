package rasteplads.messageCacheTest

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test
import rasteplads.messageCache.MessageCache

class MessageCacheTest {
    @Test
    fun `test outdated string message`() {
        val cache = MessageCache<String>(6000)
        cache.cacheMessage("first")
        Thread.sleep(6_100L)
        cache.cacheMessage("second")

        assertFalse(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun `test lifetime message`() {
        val cache1 = MessageCache<String>(3000)
        val cache2 = MessageCache<String>(6000)
        cache1.cacheMessage("first")
        cache2.cacheMessage("first2")
        Thread.sleep(3_100L)
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
    fun `test change cache time`() {
        val cache = MessageCache<String>(3000)
        cache.cacheMessage("first")
        cache.changeCacheTime(6000)
        cache.cacheMessage("second")
        Thread.sleep(3_100L)
        cache.cacheMessage("third")

        assertFalse(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assert(cache.containsMessage("third"))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun `test no outdated string message`() {
        val cache = MessageCache<String>(6000)
        cache.cacheMessage("first")
        cache.cacheMessage("second")

        assert(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun `test duplicate string key`() {
        val cache = MessageCache<String>(6000)
        cache.cacheMessage("first")
        cache.cacheMessage("first")

        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun `test duplicate int key`() {
        val cache = MessageCache<Int>(6000)
        cache.cacheMessage(1)
        cache.cacheMessage(1)

        assertEquals(cache.getSize(), 1)
    }

    @Test
    fun `test no outdated int message`() {
        val cache = MessageCache<Int>(6000)
        cache.cacheMessage(1)
        cache.cacheMessage(2)

        assert(cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assertEquals(cache.getSize(), 2)
    }

    @Test
    fun `test outdated int message`() {
        val cache = MessageCache<Int>(6000)
        cache.cacheMessage(1)
        Thread.sleep(6_100L)
        cache.cacheMessage(2)

        assertFalse(cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assertEquals(cache.getSize(), 1)
    }
}
