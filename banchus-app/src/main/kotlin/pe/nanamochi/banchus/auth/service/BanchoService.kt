package pe.nanamochi.banchus.auth.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InvalidToken
import pe.nanamochi.banchus.core.error.SessionExpired
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.infrastructure.protocol.PacketHandler
import pe.nanamochi.banchus.packets.server.RestartPacket
import pe.nanamochi.banchus.protocol.PacketReader
import pe.nanamochi.banchus.protocol.PacketWriter

private const val RECONNECT_DELAY_MS = 750

@Service
class BanchoService(
    private val sessionService: SessionService,
    private val streamService: StreamService,
    private val packetReader: PacketReader,
    private val packetHandler: PacketHandler,
    private val packetWriter: PacketWriter,
) {
    fun handlePackets(token: String, body: ByteArray): Result<ByteArray, DomainMessage> = binding {
        val uuid = UUID.fromString(token) ?: Err(InvalidToken).bind()
        val session =
            sessionService.fetchOne(uuid)
                ?: Err(
                        SessionExpired(
                            packetWriter.serializeAll(listOf(RestartPacket(RECONNECT_DELAY_MS)))
                        )
                    )
                    .bind()
        val responseStream = ByteArrayOutputStream()

        if (body.isNotEmpty()) {
            packetReader.readPackets(body).forEach { packet ->
                packetHandler.handle(packet, session, responseStream)
            }
        }

        val pendingData = streamService.readPendingData(session)
        responseStream.write(pendingData)
        responseStream.toByteArray()
    }
}
