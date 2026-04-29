package pe.nanamochi.banchus.infrastructure.redis

import java.util.concurrent.TimeUnit
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisDistributedLock(private val redisTemplate: RedisTemplate<String, String>) {
    fun acquireLock(lockKey: String, timeout: Long, unit: TimeUnit): Boolean {
        return redisTemplate.opsForValue().setIfAbsent(lockKey, "1", timeout, unit) ?: false
    }

    fun releaseLock(lockKey: String) {
        redisTemplate.delete(lockKey)
    }
}
