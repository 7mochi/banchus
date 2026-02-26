package pe.nanamochi.banchus.service

import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.protocol.PacketWriter

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
