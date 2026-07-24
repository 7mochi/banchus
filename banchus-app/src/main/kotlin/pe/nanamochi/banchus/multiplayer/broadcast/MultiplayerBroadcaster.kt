package pe.nanamochi.banchus.multiplayer.broadcast

import java.util.UUID
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.infrastructure.util.asBancho
import pe.nanamochi.banchus.multiplayer.entity.MultiplayerMatch
import pe.nanamochi.banchus.multiplayer.entity.MultiplayerMatchSlot
import pe.nanamochi.banchus.packets.server.AnnouncePacket
import pe.nanamochi.banchus.packets.server.MatchAllPlayersLoadedPacket
import pe.nanamochi.banchus.packets.server.MatchCompletePacket
import pe.nanamochi.banchus.packets.server.MatchDisbandPacket
import pe.nanamochi.banchus.packets.server.MatchJoinFailPacket
import pe.nanamochi.banchus.packets.server.MatchPlayerFailedPacket
import pe.nanamochi.banchus.packets.server.MatchPlayerSkippedPacket
import pe.nanamochi.banchus.packets.server.MatchSkipPacket
import pe.nanamochi.banchus.packets.server.MatchStartPacket
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket
import pe.nanamochi.banchus.packets.server.NewMatchPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
class MultiplayerBroadcaster(
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) {
    fun newMatch(match: MultiplayerMatch, slots: List<MultiplayerMatchSlot>) {
        streamService.broadcastData(
            StreamName.Lobby,
            packetWriter.serialize(NewMatchPacket(match.asBancho(slots))),
        )
    }

    fun matchDisbanded(matchId: Long) {
        streamService.broadcastData(
            StreamName.Lobby,
            packetWriter.serialize(MatchDisbandPacket(matchId = matchId.toInt())),
        )
    }

    fun matchUpdate(
        match: MultiplayerMatch,
        slots: List<MultiplayerMatchSlot>,
        lobby: Boolean = true,
        multiplayer: Boolean = true,
    ) {
        val data = packetWriter.serialize(MatchUpdatePacket(match.asBancho(slots)))
        if (lobby) streamService.broadcastData(StreamName.Lobby, data)
        if (multiplayer) streamService.broadcastData(StreamName.Multiplayer(match.matchId), data)
    }

    fun matchStart(matchId: Long, match: MultiplayerMatch, slots: List<MultiplayerMatchSlot>) {
        streamService.broadcastData(
            StreamName.Multiplayer(matchId),
            packetWriter.serialize(MatchStartPacket(match.asBancho(slots))),
        )
        streamService.broadcastData(
            StreamName.Lobby,
            packetWriter.serialize(MatchUpdatePacket(match.asBancho(slots))),
        )
    }

    fun matchComplete(matchId: Long) {
        streamService.broadcastData(
            StreamName.Multiplaying(matchId),
            packetWriter.serialize(MatchCompletePacket()),
        )
    }

    fun joinFail(sessionId: UUID) {
        streamService.broadcastData(
            StreamName.User(sessionId),
            packetWriter.serialize(MatchJoinFailPacket()),
        )
    }

    fun kicked(sessionId: UUID) {
        streamService.broadcastData(
            StreamName.User(sessionId),
            packetWriter.serialize(MatchJoinFailPacket()),
        )
        streamService.broadcastData(
            StreamName.User(sessionId),
            packetWriter.serialize(AnnouncePacket("You have been kicked out of the match!")),
        )
    }

    fun allPlayersLoaded(matchId: Long) {
        streamService.broadcastData(
            StreamName.Multiplaying(matchId),
            packetWriter.serialize(MatchAllPlayersLoadedPacket()),
        )
    }

    fun playerSkipped(matchId: Long, slotId: Int, allSkipped: Boolean) {
        val packet = if (allSkipped) MatchSkipPacket() else MatchPlayerSkippedPacket(slotId)
        streamService.broadcastData(
            StreamName.Multiplaying(matchId),
            packetWriter.serialize(packet),
        )
    }

    fun playerFailed(matchId: Long, slotId: Int) {
        streamService.broadcastData(
            StreamName.Multiplaying(matchId),
            packetWriter.serialize(MatchPlayerFailedPacket(slotId)),
        )
    }

    fun userPanelToMain(panels: List<ServerPacket>) {
        panels.forEach { packet ->
            streamService.broadcastData(StreamName.Main, packetWriter.serialize(packet))
        }
    }
}
