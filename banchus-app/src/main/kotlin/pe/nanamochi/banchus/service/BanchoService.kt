package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.domain.error.DomainMessage
import pe.nanamochi.banchus.domain.error.InvalidToken
import pe.nanamochi.banchus.infrastructure.protocol.PacketHandler
import pe.nanamochi.banchus.protocol.PacketReader

@Service
class BanchoService(
    private val sessionService: SessionService,
    private val streamService: StreamService,
    private val packetReader: PacketReader,
    private val packetHandler: PacketHandler,
) {
    fun handlePackets(token: String, body: ByteArray): Result<ByteArray, DomainMessage> = binding {
        val uuid = runCatching { UUID.fromString(token) }.getOrElse { Err(InvalidToken).bind() }
        val session = sessionService.fetchOne(uuid)
        val responseStream = ByteArrayOutputStream()

        if (body.isNotEmpty()) {
            packetReader.readPackets(body).forEach { packet ->
                packetHandler.handle(packet, session!!, responseStream)
            }
        }

        val pendingData = streamService.readPendingData(session!!)
        responseStream.write(pendingData)
        responseStream.toByteArray()
    }
}
