package pe.nanamochi.banchus.multiplayer.handlers

import com.github.michaelbull.result.onSuccess
import java.io.ByteArrayOutputStream
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.components.SlotStatus
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.core.enums.Mode
import pe.nanamochi.banchus.infrastructure.protocol.AbstractPacketHandler
import pe.nanamochi.banchus.infrastructure.protocol.HandleClientPacket
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.client.CreateMatchPacket
import pe.nanamochi.banchus.packets.server.MatchJoinSuccessPacket
import pe.nanamochi.banchus.protocol.PacketWriter

@Component
@HandleClientPacket(type = PacketType.OSU_CREATE_MATCH)
class CreateMatchHandler(
    private val multiplayerService: MultiplayerService,
    private val packetWriter: PacketWriter,
) : AbstractPacketHandler<CreateMatchPacket>(PacketType.OSU_CREATE_MATCH) {
    override fun handle(
        packet: CreateMatchPacket,
        session: Session,
        responseStream: ByteArrayOutputStream,
    ) {
        val matchData = packet.match
        val maxPlayerCount =
            matchData.slots.count { slot ->
                SlotStatus.fromValue(slot.status.toInt()) != SlotStatus.LOCKED
            }
        multiplayerService
            .create(
                session,
                matchData.name,
                matchData.password,
                matchData.beatmapName,
                matchData.beatmapMd5,
                matchData.beatmapId,
                Mode.fromValue(matchData.mode.value),
                maxPlayerCount,
            )
            .onSuccess { mpMatch ->
                matchData.id = mpMatch.inGameMatchId().toInt()
                matchData.slots[0].status = SlotStatus.NOT_READY.value.toByte()
                matchData.slots[0].userId = session.userId
                responseStream.write(packetWriter.serialize(MatchJoinSuccessPacket(matchData)))
            }
    }
}
