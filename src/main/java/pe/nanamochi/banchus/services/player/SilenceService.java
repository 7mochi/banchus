package pe.nanamochi.banchus.services.player;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SilenceService {
  private static final Pattern DURATION_PATTERN =
      Pattern.compile(
          "(\\d+)\\s*(y|year|mo|month|w|week|d|day|h|hour|m|min|minute|s|sec|second)s?",
          Pattern.CASE_INSENSITIVE);
  private final Clock clock;

  public Instant calculateSilenceUntil(String input) {
    Duration duration = parseDurationText(input);
    if (duration == null) {
      return null;
    }
    return Instant.now(clock).plus(duration);
  }

  public String formatRemainingSilence(Instant until) {
    Instant now = Instant.now(clock);
    Duration duration = Duration.between(now, until);
    long seconds = duration.toSeconds();

    if (seconds < 60) return format(seconds, "second");
    if (seconds < 3600) return format(seconds / 60, "minute");
    if (seconds < 86400) return format(seconds / 3600, "hour");
    if (seconds < 604800) return format(seconds / 86400, "day");
    if (seconds < 2592000) return format(seconds / 604800, "week");
    if (seconds < 31536000) return format(seconds / 2592000, "month");

    return format(seconds / 31536000, "year");
  }

  private String format(long value, String unit) {
    return value + " " + unit + (value != 1 ? "s" : "");
  }

  private Duration parseDurationText(String input) {
    if (input == null || input.isBlank()) {
      return null;
    }

    Matcher matcher = DURATION_PATTERN.matcher(input.trim());

    Duration totalDuration = Duration.ZERO;
    boolean foundAny = false;

    while (matcher.find()) {
      foundAny = true;

      long amount = Long.parseLong(matcher.group(1));
      String unit = matcher.group(2).toLowerCase();

      Duration unitDuration = parseUnit(amount, unit);
      if (unitDuration == null) {
        return null;
      }

      totalDuration = totalDuration.plus(unitDuration);
    }

    return foundAny ? totalDuration : null;
  }

  private Duration parseUnit(long amount, String unit) {
    return switch (unit) {
      case "s", "sec", "second" -> Duration.ofSeconds(amount);
      case "m", "min", "minute" -> Duration.ofMinutes(amount);
      case "h", "hour" -> Duration.ofHours(amount);
      case "d", "day" -> Duration.ofDays(amount);
      case "w", "week" -> Duration.ofDays(amount * 7);
      case "mo", "month" -> Duration.ofDays(amount * 30);
      case "y", "year" -> Duration.ofDays(amount * 365);
      default -> null;
    };
  }
}
