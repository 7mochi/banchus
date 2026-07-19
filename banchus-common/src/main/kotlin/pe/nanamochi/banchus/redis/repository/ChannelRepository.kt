package pe.nanamochi.banchus.redis.repository

import java.util.UUID
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.ChannelName

@Repository("ChannelRedisRepository")
class ChannelRepository(private val redisTemplate: RedisTemplate<String, String>) {
    fun fetchSessionChannels(sessionId: UUID): List<String> {
        return redisTemplate.opsForSet().members(makeSessionChannelsKey(sessionId))?.toList()
            ?: emptyList()
    }

    fun fetchChannelMembers(channelName: ChannelName): List<UUID> {
        return redisTemplate.opsForSet().members(makeChannelMembersKey(channelName))?.mapNotNull {
            it.toUUIDOrNull()
        } ?: emptyList()
    }

    fun memberCount(channelName: ChannelName): Long =
        redisTemplate.opsForSet().size(makeChannelMembersKey(channelName)) ?: 0L

    fun join(sessionId: UUID, channelName: ChannelName): Long {
        val sessionChannelsKey = makeSessionChannelsKey(sessionId)
        val membersKey = makeChannelMembersKey(channelName)

        redisTemplate.opsForSet().add(sessionChannelsKey, channelName.resolve())
        redisTemplate.opsForSet().add(membersKey, sessionId.toString())

        return redisTemplate.opsForSet().size(membersKey) ?: 0L
    }

    fun leave(sessionId: UUID, channelName: ChannelName): Long {
        val sessionChannelsKey = makeSessionChannelsKey(sessionId)
        val membersKey = makeChannelMembersKey(channelName)

        redisTemplate.opsForSet().remove(sessionChannelsKey, channelName.resolve())
        redisTemplate.opsForSet().remove(membersKey, sessionId.toString())

        return redisTemplate.opsForSet().size(membersKey) ?: 0L
    }

    fun clearSessionChannels(sessionId: UUID) =
        redisTemplate.delete(makeSessionChannelsKey(sessionId))

    fun String.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

    private fun makeChannelMembersKey(channelName: ChannelName) =
        "banchus:channels:$channelName:members"

    private fun makeSessionChannelsKey(sessionId: UUID) = "banchus:sessions:$sessionId:channels"
}
