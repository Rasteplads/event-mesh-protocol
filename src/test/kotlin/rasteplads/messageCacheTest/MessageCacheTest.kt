package rasteplads.messageCacheTest

import org.example.cache.MessageCache
import org.junit.jupiter.api.Test
import java.time.LocalTime

class MessageCacheTest {
    @Test
    fun testOutdatedStringMessage(){
        val cache =  MessageCache<String>()
        cache.cacheMessage(1,"first")
        Thread.sleep(60000L)
        cache.cacheMessage(2,"second")

        assert(cache.getCachedMessage(1) == null)
        assert(cache.getCachedMessage(2)?.second == "second")
        assert(cache.getCachedMessage(2)?.first?.compareTo(LocalTime.now()) == 1)

    }
    @Test
    fun testNoOutdatedStringMessage(){
        val cache =  MessageCache<String>()
        cache.cacheMessage(1,"first")
        Thread.sleep(30000L)
        cache.cacheMessage(2,"second")

        assert(cache.getCachedMessage(1)?.second == "first")
        assert(cache.getCachedMessage(1)?.first?.compareTo(LocalTime.now()) == 1)
        assert(cache.getCachedMessage(2)?.second == "second")
        assert(cache.getCachedMessage(2)?.first?.compareTo(LocalTime.now()) == 1)
    }

    @Test
    fun testDublicateKey(){
        val cache = MessageCache<String>()
        cache.cacheMessage(1, "first")
        cache.cacheMessage(1, "first2")

        assert(cache.getCachedMessage(1)?.second == "first")
        assert(cache.getCachedMessage(1)?.second != "first2")
    }
    @Test
    fun testNoOutdatedIntMessage(){
        val cache =  MessageCache<Int>()
        cache.cacheMessage(1,1)
        Thread.sleep(30000L)
        cache.cacheMessage(2,2)

        assert(cache.getCachedMessage(1)?.second == 1)
        assert(cache.getCachedMessage(1)?.first?.compareTo(LocalTime.now()) == 1)
        assert(cache.getCachedMessage(2)?.second == 2)
        assert(cache.getCachedMessage(2)?.first?.compareTo(LocalTime.now()) == 1)
    }
    @Test
    fun testOutdatedIntMessage(){
        val cache =  MessageCache<Int>()
        cache.cacheMessage(1,1)
        Thread.sleep(60000L)
        cache.cacheMessage(2,2)

        assert(cache.getCachedMessage(1) == null)
        assert(cache.getCachedMessage(2)?.second == 2)
        assert(cache.getCachedMessage(2)?.first?.compareTo(LocalTime.now()) == 1)

    }
}