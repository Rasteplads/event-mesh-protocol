package rasteplads.messageCacheTest

import org.example.cache.MessageCache
import org.junit.jupiter.api.Test

class MessageCacheTest {
    @Test
    fun testOutdatedStringMessage() {
        val cache = MessageCache<String>()
        cache.cacheMessage("first")
        Thread.sleep(60000L)
        cache.cacheMessage("second")

        assert(!cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assert(cache.getsize() == 1)
    }

    @Test
    fun testNoOutdatedStringMessage() {
        val cache = MessageCache<String>()
        cache.cacheMessage("first")
        cache.cacheMessage("second")

        assert(cache.containsMessage("first"))
        assert(cache.containsMessage("second"))
        assert(cache.getsize() == 2)
    }

    @Test
    fun testDuplicateStringKey() {
        val cache = MessageCache<String>()
        cache.cacheMessage("first")
        cache.cacheMessage("first")

        assert(cache.getsize() == 1)
    }

    @Test
    fun testDuplicateIntKey() {
        val cache = MessageCache<Int>()
        cache.cacheMessage(1)
        cache.cacheMessage(1)

        assert(cache.getsize() == 1)
    }

    @Test
    fun testNoOutdatedIntMessage() {
        val cache = MessageCache<Int>()
        cache.cacheMessage(1)
        cache.cacheMessage(2)

        assert(cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assert(cache.getsize() == 2)
    }

    @Test
    fun testOutdatedIntMessage() {
        val cache = MessageCache<Int>()
        cache.cacheMessage(1)
        Thread.sleep(60000L)
        cache.cacheMessage(2)

        assert(!cache.containsMessage(1))
        assert(cache.containsMessage(2))
        assert(cache.getsize() == 1)
    }
}
