package pe.nanamochi.banchus.service

import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.redis.repository.RankingRepository

@Service
class RankingService(private val rankingRepository: RankingRepository) {
    fun getGlobalRank(mode: Mode, user: User): UInt = rankingRepository.getGlobalRank(mode, user)

    fun getCountryRank(mode: Mode, user: User, countryCode: CountryCode): UInt =
        rankingRepository.getCountryRank(mode, user, countryCode)

    fun update(mode: Mode, user: User, stat: Stat) =
        rankingRepository.updateRanking(mode, user, stat)
}
