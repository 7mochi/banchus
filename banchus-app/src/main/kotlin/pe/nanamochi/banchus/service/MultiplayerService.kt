package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.toResultOr
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import pe.nanamochi.banchus.domain.errors.MatchNotFound
import pe.nanamochi.banchus.domain.errors.MultiplayerError
import pe.nanamochi.banchus.domain.errors.SlotNotFound
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.MultiplayerMatch
import pe.nanamochi.banchus.redis.entity.MultiplayerSlot
import pe.nanamochi.banchus.redis.repository.MultiplayerRepository

@Service
class MultiplayerService(
    private val matchBroadcastService: MatchBroadcastService,
    private val multiplayerRepository: MultiplayerRepository,
    private val channelService: ChannelService,
    private val sessionService: SessionService,
    private val packetWriter: PacketWriter,
    private val packetBundleService: PacketBundleService,
    private val spectatorService: SpectatorService,
    private val transactionTemplate: TransactionTemplate,
) {
    fun findById(id: Int): Result<MultiplayerMatch, MatchNotFound> =
        multiplayerRepository.findById(id).toResultOr { MatchNotFound }

    fun findSlotBySessionId(
        matchId: Int,
        sessionId: UUID,
    ): Result<MultiplayerSlot, MultiplayerError> {
        return findById(matchId).andThen { match ->
            match.slots.find { it.sessionId == sessionId }.toResultOr { SlotNotFound }
        }
    }

    fun fetchAll(): List<MultiplayerMatch> = multiplayerRepository.fetchAll()

    fun fetchAllSlots(matchId: Int): List<MultiplayerSlot> =
        multiplayerRepository.fetchAllSlots(matchId)

    private fun update(match: MultiplayerMatch) = multiplayerRepository.update(match)
}
