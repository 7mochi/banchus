package pe.nanamochi.banchus.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.packets.server.*;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.*;

@Slf4j
@Component
@Command(name = "silence", documentation = "Silence a user for a specified duration.")
@RequiredArgsConstructor
public class SilenceCommand extends BaseCommand {
  private final UserService userService;
  private final SilenceService silenceService;
  private final PacketWriter packetWriter;
  private final SessionService sessionService;
  private final MultiplayerService multiplayerService;
  private final ChannelService channelService;
  private final PacketBundleService packetBundleService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  String processCommand(User admin, String trigger, String[] args) {
    if (args.length < 2) {
      return "Not enough arguments. Usage: !silence <username> <duration>";
    }

    String targetUsername = args[0];
    String durationInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

    return userService
        .findByUsername(targetUsername)
        .filter(target -> !target.isRestricted())
        .map(
            target -> {
              try {
                return executeSilence(admin, target, durationInput);
              } catch (IOException e) {
                log.error("Error executing silence command", e);
                return "Internal error executing silence.";
              }
            })
        .orElse("Username not found.");
  }

  private String executeSilence(User admin, User target, String durationInput) throws IOException {
    if (target.getId() == admin.getId()) return "You cannot silence yourself.";
    if (target.getId() == 1) return "You cannot silence the bot.";
    if (target.isSilenced()) return "User is already silenced.";

    Instant silencedUntil = silenceService.calculateSilenceUntil(durationInput);
    if (silencedUntil == null) {
      return "Invalid duration format (e.g., 10m, 1h, 2d).";
    }

    target.setSilenceEnd(silencedUntil);
    userService.update(target);

    // Notify target's primary session if online
    sessionService
        .findPrimaryByUserId(target.getId())
        .ifPresent(
            targetSession -> {
              try {
                notifyTargetOfSilence(targetSession, silencedUntil);
                handleMultiplayerCleanup(targetSession);
              } catch (IOException e) {
                log.error("Error notifying target session", e);
              }
            });

    broadcastSilenceToAll(target.getId());

    return String.format(
        "User %s has been silenced for %s.",
        target.getUsername(), silenceService.formatRemainingSilence(silencedUntil));
  }

  private void notifyTargetOfSilence(Session session, Instant until) throws IOException {
    long seconds = Math.max(0, Duration.between(Instant.now(), until).toSeconds());
    byte[] data = serialize(new SilenceInfoPacket(Math.toIntExact(seconds)));
    packetBundleService.enqueue(session.getId(), new PacketBundle(data));
  }

  private void broadcastSilenceToAll(int userId) throws IOException {
    byte[] data = serialize(new UserSilencedPacket(userId));
    sessionService
        .findAll()
        .forEach(s -> packetBundleService.enqueue(s.getId(), new PacketBundle(data)));
  }

  private void handleMultiplayerCleanup(Session session) throws IOException {
    if (session.getMultiplayerMatchId() == null || session.getMultiplayerMatchId() == -1) return;

    int matchId = session.getMultiplayerMatchId();

    multiplayerService
        .findById(matchId)
        .flatMap(_ -> multiplayerService.findSlotBySessionId(matchId, session.getId()))
        .ifPresent(
            slot -> {
              MultiplayerMatch updatedMatch =
                  multiplayerService.resetSlot(matchId, slot.getSlotId());
              if (updatedMatch != null) {
                // Host logic and match closure
                if (updatedMatch.getHostUserId() == session.getUser().getId()) {
                  handleHostDeparture(updatedMatch, session);
                } else {
                  matchBroadcastService.broadcastMatchUpdates(
                      updatedMatch.getMatchId(), true, List.of());
                }
              }
            });
  }

  private void handleHostDeparture(MultiplayerMatch match, Session session) {
    List<pe.nanamochi.banchus.redis.model.MultiplayerSlot> slots =
        multiplayerService.getAllSlots(match.getMatchId());

    // Find new host (first slot with player)
    slots.stream()
        .filter(s -> s.getUserId() != -1)
        .findFirst()
        .ifPresentOrElse(
            newHost -> {
              match.setHostUserId(newHost.getUserId());
              multiplayerService.update(match);
              matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());
            },
            () -> disbandMatch(match, session) // No players left
            );
  }

  private void disbandMatch(MultiplayerMatch match, Session session) {
    try {
      byte[] disbandData = serialize(new MatchDisbandPacket(match.getMatchId()));
      matchBroadcastService.broadcastToLobby(disbandData);

      channelService
          .findByName("#mp_" + match.getMatchId())
          .ifPresent(
              chan -> {
                channelService.leaveChannel(chan, session);
                channelService.delete(chan);
              });

      multiplayerService.deleteMatch(match.getMatchId());
      log.info("Match {} disbanded after silence of host", match.getMatchId());
    } catch (IOException e) {
      log.error("Error disbanding match", e);
    }
  }

  private byte[] serialize(ServerPacket packet) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    packetWriter.writePacket(os, packet);
    return os.toByteArray();
  }
}
