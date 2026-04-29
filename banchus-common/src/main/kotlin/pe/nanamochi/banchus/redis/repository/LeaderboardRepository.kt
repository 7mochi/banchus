package pe.nanamochi.banchus.redis.repository

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode

@Repository
class LeaderboardRepository(private val redisTemplate: RedisTemplate<String, String>) {
    fun addToLeaderboard(user: User, mode: Mode, performancePoints: Int) {
        redisTemplate
            .opsForZSet()
            .add(makeKey(mode), user.id.toString(), performancePoints.toDouble())
        if (user.country != CountryCode.XX) {
            redisTemplate
                .opsForZSet()
                .add(
                    makeCountryKey(mode, user.country),
                    user.id.toString(),
                    performancePoints.toDouble(),
                )
        }
    }

    fun removeFromLeaderboard(user: User, mode: Mode) {
        redisTemplate.opsForZSet().remove(makeKey(mode), user.id.toString())
        if (user.country != CountryCode.XX) {
            redisTemplate
                .opsForZSet()
                .remove(makeCountryKey(mode, user.country), user.id.toString())
        }
    }

    fun fetchGlobalRank(userId: Int, mode: Mode): UInt =
        redisTemplate.opsForZSet().reverseRank(makeKey(mode), userId.toString())?.toUInt() ?: 0u

    fun fetchCountryRank(userId: Int, mode: Mode, countryCode: CountryCode): UInt =
        redisTemplate
            .opsForZSet()
            .reverseRank(makeCountryKey(mode, countryCode), userId.toString())
            ?.toUInt() ?: 0u

    private fun makeKey(mode: Mode) = "banchus:leaderboard:${mode.alias}"

    private fun makeCountryKey(mode: Mode, countryCode: CountryCode) =
        "banchus:country:${mode.alias}:${countryCode.code}"
}
