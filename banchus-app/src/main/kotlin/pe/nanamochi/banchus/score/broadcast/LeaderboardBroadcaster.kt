package pe.nanamochi.banchus.score.broadcast

import org.springframework.stereotype.Component
import pe.nanamochi.banchus.beatmap.entity.Beatmap
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.packets.server.MessagePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.score.entity.Score
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.enums.Mods
import pe.nanamochi.banchus.core.service.StreamService

@Component
class LeaderboardBroadcaster(
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) {
    fun announceFirstPlace(beatmap: Beatmap, score: Score, user: User) {
        streamService.broadcastData(
            StreamName.Channel("#announce"),
            packetWriter.serialize(
                MessagePacket(
                    sender = "BanchoBot",
                    content =
                        "${user.username} has achieved #1 on ${beatmap.beatmapset?.artist} - ${beatmap.beatmapset?.title} [${beatmap.version}] +${Mods.fromBitmask(score.mods.toUInt())} (${"%.2f".format(score.performancePoints)}pp)",
                    target = "#announce",
                    senderId = 1,
                )
            ),
        )
    }
}
