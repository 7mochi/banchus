package pe.nanamochi.banchus.core.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.service.SessionService
import pe.nanamochi.banchus.chat.service.MessageService
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.entity.Presence
import pe.nanamochi.banchus.core.error.BotSilenceNotAllowed
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.InvalidDuration
import pe.nanamochi.banchus.core.error.SelfSilenceNotAllowed
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.identity.service.UserService
import pe.nanamochi.banchus.packets.server.SilenceInfoPacket
import pe.nanamochi.banchus.packets.server.UserSilencedPacket
import pe.nanamochi.banchus.protocol.PacketWriter

private val SILENCE_AUTO_DELETE_INTERVAL = Duration.ofSeconds(60)
private val AUTO_SILENCE_DURATION = Duration.ofMinutes(5)

@Service
class SilenceService(
    private val userService: UserService,
    private val messageService: MessageService,
    private val sessionService: SessionService,
    private val packetWriter: PacketWriter,
    private val streamService: StreamService,
) {
    private val durationPattern =
        Pattern.compile(
            "(\\d+)\\s*(y|year|mo|month|w|week|d|day|h|hour|m|min|minute|s|sec|second)s?",
            Pattern.CASE_INSENSITIVE,
        )

    fun autoSilence(user: User): Result<Unit, DomainMessage> = binding {
        if (user.id == Presence.BOT_ID) Err(BotSilenceNotAllowed).bind()
        applySilence(user, AUTO_SILENCE_DURATION).bind()
    }

    fun silenceUser(adminId: Int, user: User, duration: String): Result<Unit, DomainMessage> =
        binding {
            if (user.id == adminId) Err(SelfSilenceNotAllowed).bind()
            val parsed = parseDurationText(duration).toResultOr { InvalidDuration }.bind()
            applySilence(user, parsed).bind()
        }

    private fun applySilence(user: User, duration: Duration): Result<Unit, DomainMessage> =
        binding {
            val silencedUntil = Instant.now().plus(duration)
            user.silenceEnd = silencedUntil
            userService.update(user).bind()

            messageService.softDeleteRecent(user.id, SILENCE_AUTO_DELETE_INTERVAL)
            sessionService.fetchByUserId(user.id).forEach { session ->
                sessionService.silence(session, duration)
                val seconds =
                    Duration.between(Instant.now(), silencedUntil).seconds.coerceAtLeast(0)
                streamService.broadcastData(
                    StreamName.User(session.sessionId),
                    packetWriter.serialize(SilenceInfoPacket(silenceLength = seconds.toInt())),
                )
            }

            streamService.broadcastData(
                StreamName.Main,
                packetWriter.serialize(UserSilencedPacket(user.id)),
            )
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
