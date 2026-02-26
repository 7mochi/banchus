package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InvalidToken
import pe.nanamochi.banchus.infrastructure.protocol.PacketHandler
import pe.nanamochi.banchus.protocol.PacketReader

@Service
class BanchoService(
    private val sessionService: SessionService,
    private val packetBundleService: PacketBundleService,
    private val packetReader: PacketReader,
    private val packetHandler: PacketHandler,
) {
    fun handlePackets(token: String, body: ByteArray): Result<ByteArray, DomainMessage> = binding {
        val uuid = runCatching { UUID.fromString(token) }.getOrElse { Err(InvalidToken).bind() }

        val session = sessionService.findById(uuid).bind()

        val responseStream = ByteArrayOutputStream()

        if (body.isNotEmpty()) {
            packetReader.readPackets(body).filterIsInstance<BanchoPacket.Client>().forEach { packet
                ->
                packetHandler.handle(packet, session, responseStream)
            }
        }

        packetBundleService.dequeueAll(uuid).forEach { bundle -> responseStream.write(bundle.data) }

        responseStream.toByteArray()
    }
}
