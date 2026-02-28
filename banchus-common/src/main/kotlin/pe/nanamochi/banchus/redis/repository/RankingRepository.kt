package pe.nanamochi.banchus.redis.repository

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode

@Repository
class RankingRepository(private val redisTemplate: RedisTemplate<String, String>) {
    fun getGlobalRank(mode: Mode, user: User): UInt =
        user
            .takeIf { !it.isRestricted }
            ?.let {
                redisTemplate
                    .opsForZSet()
                    .reverseRank(makeKeyGlobal(mode), it.id.toString())
                    ?.plus(1)
                    ?.toUInt() ?: 0u
            } ?: 0u

    fun getCountryRank(mode: Mode, user: User, countryCode: CountryCode): UInt =
        user
            .takeIf { !it.isRestricted }
            ?.let {
                redisTemplate
                    .opsForZSet()
                    .reverseRank(makeKeyCountry(mode, countryCode), it.id.toString())
                    ?.plus(1)
                    ?.toUInt() ?: 0u
            } ?: 0u

    fun updateRanking(mode: Mode, user: User, stat: Stat): UInt =
        user
            .takeIf { !it.isRestricted }
            ?.let { u ->
                val userId = u.id.toString()
                val pp = stat.performancePoints.toDouble()

                redisTemplate.opsForZSet().add(makeKeyGlobal(mode), userId, pp)
                redisTemplate.opsForZSet().add(makeKeyCountry(mode, u.country), userId, pp)

                getGlobalRank(mode, u)
            } ?: 0u

    fun makeKeyGlobal(mode: Mode): String = "server:ranking:${mode.value}"

    fun makeKeyCountry(mode: Mode, countryCode: CountryCode): String =
        "server:ranking:${mode.value}:${countryCode.code}"
}
