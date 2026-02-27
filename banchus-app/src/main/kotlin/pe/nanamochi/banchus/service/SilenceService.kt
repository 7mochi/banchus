package pe.nanamochi.banchus.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import jakarta.transaction.Transactional
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.errors.BotSilenceNotAllowed
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InvalidDuration
import pe.nanamochi.banchus.domain.errors.SelfSilenceNotAllowed
import pe.nanamochi.banchus.domain.errors.UserSilenced
import pe.nanamochi.banchus.packets.server.SilenceInfoPacket
import pe.nanamochi.banchus.packets.server.UserSilencedPacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.redis.entity.PacketBundle

@Service
class SilenceService(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val multiplayerService: MultiplayerService,
    private val packetWriter: PacketWriter,
    private val packetBundleService: PacketBundleService,
) {
    private val durationPattern =
        Pattern.compile(
            "(\\d+)\\s*(y|year|mo|month|w|week|d|day|h|hour|m|min|minute|s|sec|second)s?",
            Pattern.CASE_INSENSITIVE,
        )

    @Transactional
    fun silenceUser(admin: User, target: User, durationInput: String): Result<Unit, DomainMessage> =
        binding {
            if (target.id == admin.id) Err(SelfSilenceNotAllowed).bind()
            if (target.id == 1) Err(BotSilenceNotAllowed).bind()
            if (target.isSilenced) Err(UserSilenced).bind()

            val duration = parseDurationText(durationInput).toResultOr { InvalidDuration }.bind()
            val silencedUntil = Instant.now().plus(duration)

            target.silenceEnd = silencedUntil
            userService.update(target).bind()

            val session = sessionService.findPrimaryByUserId(target.id).bind()
            multiplayerService.handleUserDeparture(session)

            notifyTargetOfSilence(session, silencedUntil)
            broadcastSilenceToAll(target.id)
        }

    private fun notifyTargetOfSilence(session: Session, until: Instant) {
        val seconds = Duration.between(Instant.now(), until).seconds.coerceAtLeast(0)
        val data = packetWriter.serialize(SilenceInfoPacket(seconds.toInt()))

        packetBundleService.enqueue(session.id!!, PacketBundle(data))
    }

    private fun broadcastSilenceToAll(targetId: Int) {
        val data = packetWriter.serialize(UserSilencedPacket(targetId))

        sessionService.findAll().forEach { session ->
            packetBundleService.enqueue(session.id!!, PacketBundle(data))
        }
    }

    fun formatRemainingSilence(until: Instant): String {
        val seconds = Duration.between(Instant.now(), until).seconds
        if (seconds <= 0) return "0 seconds"

        val (value, unit) =
            when {
                seconds < 60 -> seconds to "second"
                seconds < 3600 -> (seconds / 60) to "minute"
                seconds < 86400 -> (seconds / 3600) to "hour"
                seconds < 604800 -> (seconds / 86400) to "day"
                seconds < 2592000 -> (seconds / 604800) to "week"
                seconds < 31536000 -> (seconds / 2592000) to "month"
                else -> (seconds / 31536000) to "year"
            }

        return "$value $unit${if (value != 1L) "s" else ""}"
    }

    private fun parseDurationText(input: String?): Duration? {
        if (input.isNullOrBlank()) return null

        val matcher = durationPattern.matcher(input.trim())
        var totalDuration = Duration.ZERO
        var foundAny = false

        while (matcher.find()) {
            foundAny = true
            val amount = matcher.group(1).toLong()
            val unit = matcher.group(2).lowercase()

            val unitDuration =
                when (unit) {
                    "s",
                    "sec",
                    "second" -> Duration.ofSeconds(amount)
                    "m",
                    "min",
                    "minute" -> Duration.ofMinutes(amount)
                    "h",
                    "hour" -> Duration.ofHours(amount)
                    "d",
                    "day" -> Duration.ofDays(amount)
                    "w",
                    "week" -> Duration.ofDays(amount * 7)
                    "mo",
                    "month" -> Duration.ofDays(amount * 30)
                    "y",
                    "year" -> Duration.ofDays(amount * 365)
                    else -> Duration.ZERO
                }
            totalDuration = totalDuration.plus(unitDuration)
        }

        return if (foundAny) totalDuration else null
    }
}
