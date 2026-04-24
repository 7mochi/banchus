package pe.nanamochi.banchus.service

import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.redis.repository.LeaderboardRepository

@Service
class LeaderboardService(private val leaderboardRepository: LeaderboardRepository) {
    fun addToLeaderboard(user: User, mode: Mode, performancePoints: Int) =
        leaderboardRepository.addToLeaderboard(user, mode, performancePoints)

    fun removeFromLeaderboard(user: User, mode: Mode) =
        leaderboardRepository.removeFromLeaderboard(user, mode)

    fun fetchGlobalRank(userId: Int, mode: Mode) =
        leaderboardRepository.fetchGlobalRank(userId, mode)
}
