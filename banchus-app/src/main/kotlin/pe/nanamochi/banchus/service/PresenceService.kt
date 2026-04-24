package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.infrastructure.client.IPApiClient
import pe.nanamochi.banchus.packets.client.ChangeStatusPacket
import pe.nanamochi.banchus.packets.client.PresenceRequestPacket
import pe.nanamochi.banchus.packets.client.UserStatsRequestPacket
import pe.nanamochi.banchus.packets.server.UserQuitPacket
import pe.nanamochi.banchus.packets.server.UserStatsPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.Presence
import pe.nanamochi.banchus.redis.entity.Session
import pe.nanamochi.banchus.redis.repository.PresenceRepository
import pe.nanamochi.banchus.redis.stream.StreamName
import pe.nanamochi.banchus.util.toBanchoUser
import pe.nanamochi.banchus.util.userPanel

@Service
class PresenceService(
    private val packetWriter: PacketWriter,
    private val presenceRepository: PresenceRepository,
    private val ipApiClient: IPApiClient,
    private val statService: StatService,
    private val leaderboardService: LeaderboardService,
    private val streamService: StreamService,
) {
    fun create(presence: Presence): Presence = presenceRepository.create(presence)

    fun delete(userId: Int) = presenceRepository.delete(userId)

    fun update(presence: Presence) = presenceRepository.update(presence)

    fun fetchOne(userId: Int) = presenceRepository.fetchOne(userId)

    fun fetchUserIds() = presenceRepository.fetchUserIds()

    fun fetchMultiple(userIds: List<Int>) = presenceRepository.fetchMultiple(userIds)

    fun fetchAll(): List<Presence> = presenceRepository.fetchAll()

    fun handleUserStatsRequest(
        packet: UserStatsRequestPacket,
        responseStream: ByteArrayOutputStream,
    ) {
        val presences = fetchMultiple(packet.userIds)

        packet.userIds.zip(presences).forEach { (userId, presence) ->
            presence?.let { p ->
                if (p.globalRank > 0u) {
                    responseStream.write(
                        packetWriter.serialize(UserStatsPacket(user = p.toBanchoUser()))
                    )
                }
            } ?: run { responseStream.write(packetWriter.serialize(UserQuitPacket(userId))) }
        }
    }

    fun handlePresenceRequest(
        packet: PresenceRequestPacket,
        responseStream: ByteArrayOutputStream,
    ) {
        val presences = fetchMultiple(packet.userIds)
        packet.userIds.zip(presences).forEach { (userId, presence) ->
            presence?.let { p ->
                responseStream.write(
                    packetWriter.serializeAll(p.toBanchoUser().let { user -> p.userPanel() })
                )
            } ?: run { responseStream.write(packetWriter.serialize(UserQuitPacket(userId))) }
        }
    }

    fun handlePresenceRequestAll(responseStream: ByteArrayOutputStream) {
        fetchAll().forEach { presence ->
            responseStream.write(packetWriter.serializeAll(presence.userPanel()))
        }
    }

    fun handleChangeStatus(
        packet: ChangeStatusPacket,
        session: Session,
    ): Result<Unit, DomainMessage> = binding {
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

        val refreshStats = presence.mode != packet.statusUpdate.mode.value
        presence.action = packet.statusUpdate.status.value.toUByte()
        presence.infoText = packet.statusUpdate.text
        presence.beatmapMd5 = packet.statusUpdate.beatmapMd5
        presence.beatmapId = packet.statusUpdate.beatmapId
        presence.mods = Mods.toBitmask(packet.statusUpdate.mods).toInt()
        presence.mode = packet.statusUpdate.mode.value

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

        presence = presenceRepository.update(presence)

        presence.userPanel().forEach { packet ->
            if (!session.isRestricted) {
                streamService.broadcastMessage(StreamName.Main, packet)
            }
        }
    }

    fun handleRequestStatus(
        session: Session,
        responseStream: ByteArrayOutputStream,
    ): Result<Unit, DomainMessage> = binding {
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
            return@binding
        }

        presence.rankedScore = stats.rankedScore.toULong()
        presence.totalScore = stats.totalScore.toULong()
        presence.accuracy = stats.averageAccuracy
        presence.playcount = stats.playCount.toUInt()
        presence.performancePoints = stats.performancePoints.toUInt()
        presence.globalRank = globalRank
        presence.performancePoints = stats.performancePoints.toUInt()

        val updatedPresence = presenceRepository.update(presence)

        val userStatsPacket =
            packetWriter.serialize(UserStatsPacket(updatedPresence.toBanchoUser()))

        if (!session.isRestricted) {
            streamService.broadcastData(StreamName.Main, userStatsPacket)
        } else {
            responseStream.write(userStatsPacket)
        }
    }
}
