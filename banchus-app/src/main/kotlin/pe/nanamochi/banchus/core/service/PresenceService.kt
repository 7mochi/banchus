package pe.nanamochi.banchus.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.net.InetAddress
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.components.StatusUpdate
import pe.nanamochi.banchus.core.entity.Presence
import pe.nanamochi.banchus.core.enums.CountryCode
import pe.nanamochi.banchus.core.enums.Mode
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.repository.PresenceRepository
import pe.nanamochi.banchus.infrastructure.client.IPApiClient
import pe.nanamochi.banchus.score.service.LeaderboardService
import pe.nanamochi.banchus.score.service.StatService

@Service
class PresenceService(
    private val presenceRepository: PresenceRepository,
    private val ipApiClient: IPApiClient,
    private val statService: StatService,
    private val leaderboardService: LeaderboardService,
) {
    fun create(presence: Presence): Presence = presenceRepository.create(presence)

    fun delete(userId: Int) = presenceRepository.delete(userId)

    fun update(presence: Presence) = presenceRepository.update(presence)

    fun fetchOne(userId: Int) = presenceRepository.fetchOne(userId)

    fun fetchUserIds() = presenceRepository.fetchUserIds()

    fun fetchMultiple(userIds: List<Int>) = presenceRepository.fetchMultiple(userIds)

    fun fetchAll(): List<Presence> = presenceRepository.fetchAll()

    fun getUserStats(userIds: List<Int>): List<Pair<Int, Presence?>> {
        val presences = fetchMultiple(userIds)
        return userIds.zip(presences)
    }

    fun getPresences(userIds: List<Int>): List<Pair<Int, Presence?>> {
        val presences = fetchMultiple(userIds)
        return userIds.zip(presences)
    }

    fun changeStatus(
        statusUpdate: StatusUpdate,
        session: Session,
    ): Result<Presence, DomainMessage> = binding {
        var presence =
            fetchOne(session.userId)
                ?: run {
                    val geolocation =
                        ipApiClient.fetchFromIP(InetAddress.getByName(session.createIpAddress))
                    create(
                        Presence(
                            userId = session.userId,
                            username = session.username,
                            privileges = session.privileges,
                            countryCode = CountryCode.fromCode(geolocation.countryCode),
                        )
                    )
                }

        val refreshStats = presence.mode != statusUpdate.mode.value
        presence.action = statusUpdate.status.value.toUByte()
        presence.infoText = statusUpdate.text
        presence.beatmapMd5 = statusUpdate.beatmapMd5
        presence.beatmapId = statusUpdate.beatmapId
        presence.mods = Mods.toBitmask(statusUpdate.mods).toInt()
        presence.mode = statusUpdate.mode.value

        if (refreshStats) {
            val stats = statService.fetchOne(session.userId, Mode.fromValue(presence.mode)).bind()
            val globalRank =
                leaderboardService.fetchGlobalRank(session.userId, Mode.fromValue(presence.mode))

            presence.rankedScore = stats.rankedScore.toULong()
            presence.totalScore = stats.totalScore.toULong()
            presence.accuracy = stats.averageAccuracy
            presence.playcount = stats.playCount.toUInt()
            presence.performancePoints = stats.performancePoints.toUInt()
            presence.globalRank = globalRank
        }

        presenceRepository.update(presence)
    }

    fun getRequestStatus(session: Session): Result<Presence, DomainMessage> = binding {
        val presence =
            fetchOne(session.userId)
                ?: Presence(
                    userId = session.userId,
                    username = session.username,
                    privileges = session.privileges,
                )

        val stats = statService.fetchOne(session.userId, Mode.fromValue(presence.mode)).bind()
        val globalRank =
            leaderboardService.fetchGlobalRank(session.userId, Mode.fromValue(presence.mode))

        if (
            presence.rankedScore == stats.rankedScore.toULong() &&
                presence.totalScore == stats.totalScore.toULong() &&
                presence.accuracy == stats.averageAccuracy &&
                presence.playcount == stats.playCount.toUInt() &&
                presence.globalRank == globalRank &&
                presence.performancePoints == stats.performancePoints.toUInt()
        ) {
            return@binding presence
        }

        presence.rankedScore = stats.rankedScore.toULong()
        presence.totalScore = stats.totalScore.toULong()
        presence.accuracy = stats.averageAccuracy
        presence.playcount = stats.playCount.toUInt()
        presence.performancePoints = stats.performancePoints.toUInt()
        presence.globalRank = globalRank

        presenceRepository.update(presence)
    }
}
