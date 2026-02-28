package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.Mods
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.mapper.BanchoUserMapper
import pe.nanamochi.banchus.packets.client.UserStatusPacket
import pe.nanamochi.banchus.packets.server.UserStatsPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.PacketBundle

@Service
class PresenceService(
    private val userMapper: BanchoUserMapper,
    private val sessionService: SessionService,
    private val statService: StatService,
    private val packetWriter: PacketWriter,
    private val packetBundleService: PacketBundleService,
) {
    fun updateFromStatusPacket(
        session: Session,
        packet: UserStatusPacket,
    ): Result<Unit, DomainMessage> = binding {
        // TODO: Check privileges

        // Convert from packet Mods/Mode to domain Mods/Mode
        val gamemode = Mode.fromValue(packet.mode.value)

        // Filter invalid mod combinations, this is a quirk of the osu! client,
        // where it adjusts this value only after it sends the packet to the server,
        // so we need to adjust
        val filteredMods =
            Mods.filterInvalidModCombinations(
                pe.nanamochi.banchus.components.Mods.toBitmask(packet.mods),
                gamemode,
            )

        session.apply {
            action = packet.action.value
            infoText = packet.text
            beatmapMd5 = packet.beatmapChecksum
            mods = filteredMods.toInt()
            this.gamemode = gamemode
            beatmapId = packet.beatmapId
        }
        val updatedSession = sessionService.update(session).bind()

        // Send the stats update to all active osu sessions
        broadcastStats(updatedSession).bind()
    }

    fun broadcastStats(session: Session): Result<Unit, DomainMessage> = binding {
        val user = session.user ?: Err(UserNotFound).bind()
        val stats = statService.findByUserAndGamemode(user, session.gamemode).bind()

        val bundle = createStatsBundle(session, stats).bind()
        enqueueStats(session, user, bundle)
    }

    fun broadcastStats(
        session: Session,
        user: User,
        stats: Stat,
        rank: Int,
    ): Result<Unit, DomainMessage> = binding {
        val bundle = createStatsBundle(session, stats, rank).bind()
        enqueueStats(session, user, bundle)
    }

    private fun enqueueStats(session: Session, user: User, bundle: PacketBundle) {
        val sessionId = session.id ?: return

        if (user.isRestricted) {
            packetBundleService.enqueue(sessionId, bundle)
        } else {
            sessionService.findAll().forEach { target ->
                target.id?.let { packetBundleService.enqueue(it, bundle) }
            }
        }
    }

    fun broadcastSelfStats(session: Session): Result<Unit, DomainMessage> = binding {
        val user = session.user ?: Err(UserNotFound).bind()
        val stats = statService.findByUserAndGamemode(user, session.gamemode).bind()
        val bundle = createStatsBundle(session, stats).bind()

        val sessionId = session.id ?: return@binding
        packetBundleService.enqueue(sessionId, bundle)
    }

    private fun createStatsBundle(
        session: Session,
        stats: Stat,
        forcedRank: Int? = null,
    ): Result<PacketBundle, DomainMessage> = binding {
        val packetUser = userMapper.toPacketUser(session, stats, forcedRank).bind()
        val statsPacket = UserStatsPacket(packetUser)

        PacketBundle(packetWriter.serialize(statsPacket))
    }
}
