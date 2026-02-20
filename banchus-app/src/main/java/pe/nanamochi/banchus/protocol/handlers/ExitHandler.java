package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.components.QuitState;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Channel;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.ExitPacket;
import pe.nanamochi.banchus.packets.server.ChannelAvailablePacket;
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket;
import pe.nanamochi.banchus.packets.server.MatchDisbandPacket;
import pe.nanamochi.banchus.packets.server.MatchTransferHostPacket;
import pe.nanamochi.banchus.packets.server.UserQuitPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;
import pe.nanamochi.banchus.redis.model.MultiplayerSlot;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.*;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(value = Packets.OSU_EXIT, checkForRestriction = true)
public class ExitHandler extends AbstractPacketHandler<ExitPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final ChannelService channelService;
  private final SpectatorService spectatorService;
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(ExitPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    // The osu! client will often attempt to logout as soon as they login,
    // this is a quirk of the client, and we don't really want to log them out;
    // so we ignore this case if it's been < 1 second since the client's login
    if (Duration.between(session.getCreatedAt(), Instant.now()).compareTo(Duration.ofSeconds(1))
        < 0) {
      log.debug(
          "Ignoring logout attempt < 1 second after login for user {}",
          session.getUser().getUsername());
      return;
    }

    log.info("User {} is logging out...", session.getUser().getUsername());

    // Handle the user spectating another user or being spectated
    handleSpectatorCleanup(session);

    // Handle the player being in a multiplayer match
    handleMultiplayerCleanup(session);

    // Handle the player being in any chat channels
    handleChannelCleanup(session);

    // Tell everyone else we logout (unless restricted)
    notifyUserLogout(session);

    // Delete session
    sessionService.delete(session);

    log.info("User {} has logged out successfully.", session.getUser().getUsername());
  }

  private void handleSpectatorCleanup(Session session) {
    if (session.getSpectatorHostSessionId() != null) {
      // User is spectating someone else
      spectatorService.removeSpectator(session.getSpectatorHostSessionId(), session.getId());
      log.debug("Removed user {} from spectating", session.getUser().getUsername());
    } else {
      // Handle some users spectating us
      if (spectatorService.hasSpectators(session.getId())) {
        Set<UUID> ourSpectators = spectatorService.getSpectators(session.getId());

        log.debug(
            "User {} has {} spectators, cleaning up...",
            session.getUser().getUsername(),
            ourSpectators.size());

        channelService
            .findByName("#spec_" + session.getId())
            .ifPresent(
                spectatorChannel -> {
                  // Kick all spectators
                  ourSpectators.forEach(
                      spectatorSessionId ->
                          sessionService
                              .findById(spectatorSessionId)
                              .ifPresent(
                                  spectatorSession -> {
                                    channelService.leaveChannel(spectatorChannel, spectatorSession);
                                    try {
                                      ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                      packetWriter.writePacket(
                                          stream, new ChannelRevokedPacket("#spectator"));
                                      packetBundleService.enqueue(
                                          spectatorSessionId,
                                          new PacketBundle(stream.toByteArray()));
                                    } catch (IOException e) {
                                      log.error("Error notifying spectator", e);
                                    }
                                  }));

                  // Remove us from the #spectator channel and delete it
                  spectatorService.removeAllSpectators(session.getId());
                  channelService.leaveChannel(spectatorChannel, session);
                  channelService.delete(spectatorChannel);
                  log.debug(
                      "Deleted spectator channel for user {}", session.getUser().getUsername());
                });
      }
    }
  }

  private void handleMultiplayerCleanup(Session session) {
    if (session.getMultiplayerMatchId() == null || session.getMultiplayerMatchId() == -1) {
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresent(
            match -> {
              log.debug(
                  "User {} is in match {}, cleaning up...",
                  session.getUser().getUsername(),
                  match.getMatchId());

              multiplayerService
                  .findSlotBySessionId(match.getMatchId(), session.getId())
                  .ifPresent(
                      slot -> {
                        // Reset player's slot
                        MultiplayerMatch updatedMatch =
                            multiplayerService.resetSlot(match.getMatchId(), slot.getSlotId());
                        if (updatedMatch != null
                            && updatedMatch.getHostUserId() == session.getUser().getId()) {
                          try {
                            // If the host left, pick a new host or disband match
                            handleHostLeaving(session, updatedMatch);
                          } catch (IOException e) {
                            log.error("Error handling host leaving", e);
                          }
                        } else if (updatedMatch != null) {
                          // Broadcast match updates to all players
                          matchBroadcastService.broadcastMatchUpdates(
                              match.getMatchId(), true, java.util.List.of());
                        }
                      });
            });
  }

  private void handleHostLeaving(Session session, MultiplayerMatch match) throws IOException {
    // Find new host
    MultiplayerSlot newHostSlot =
        multiplayerService.getAllSlots(match.getMatchId()).stream()
            .filter(s -> s.getUserId() != -1)
            .findFirst()
            .orElse(null);

    if (newHostSlot == null) {
      // No players left, disband match
      disbandMatch(session, match);
    } else {
      // Transfer host
      transferHost(match, newHostSlot);
    }
  }

  private void disbandMatch(Session session, MultiplayerMatch match) throws IOException {
    log.info("Disbanding match {} as no players remain", match.getMatchId());

    // Notify lobby
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new MatchDisbandPacket(match.getMatchId()));
    matchBroadcastService.broadcastToLobby(stream.toByteArray());

    // Remove user from match channel and notify
    channelService
        .findByName("#mp_" + match.getMatchId())
        .ifPresent(
            matchChannel -> {
              channelService.leaveChannel(matchChannel, session);
              try {
                ByteArrayOutputStream notifyStream = new ByteArrayOutputStream();
                packetWriter.writePacket(notifyStream, new MatchDisbandPacket(match.getMatchId()));
                packetWriter.writePacket(notifyStream, new ChannelRevokedPacket("#multiplayer"));
                packetBundleService.enqueue(
                    session.getId(), new PacketBundle(notifyStream.toByteArray()));
              } catch (IOException e) {
                log.error("Error notifying user about match disband", e);
              }

              // Delete channel
              channelService.delete(matchChannel);
            });

    // Delete match
    multiplayerService.deleteMatch(match.getMatchId());
    log.info("Match {} has been disbanded", match.getMatchId());
  }

  private void transferHost(MultiplayerMatch match, MultiplayerSlot newHostSlot)
      throws IOException {
    log.info(
        "Transferring host of match {} to user ID {}", match.getMatchId(), newHostSlot.getUserId());

    match.setHostUserId(newHostSlot.getUserId());
    multiplayerService.update(match);

    // Notify new host
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new MatchTransferHostPacket());
    packetBundleService.enqueue(newHostSlot.getSessionId(), new PacketBundle(stream.toByteArray()));

    // Broadcast updates
    matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, java.util.List.of());
  }

  private void handleChannelCleanup(Session session) {
    // Get all channels from repository
    channelService.findByAutoJoin(true).forEach(channel -> cleanupChannel(session, channel));
    channelService.findByAutoJoin(false).forEach(channel -> cleanupChannel(session, channel));
  }

  private void cleanupChannel(Session session, Channel channel) {
    Set<UUID> members = channelService.getMemberIds(channel.getId());

    if (members.contains(session.getId())) {
      channelService.leaveChannel(channel, session);

      // Update channel info for remaining members
      Set<UUID> remainingMembers = channelService.getMemberIds(channel.getId());
      remainingMembers.forEach(
          memberId -> {
            try {
              ByteArrayOutputStream stream = new ByteArrayOutputStream();
              packetWriter.writePacket(
                  stream,
                  new ChannelAvailablePacket(
                      channel.getName(), channel.getTopic(), remainingMembers.size()));
              packetBundleService.enqueue(memberId, new PacketBundle(stream.toByteArray()));
            } catch (IOException e) {
              log.error("Error notifying channel member", e);
            }
          });

      log.debug(
          "Removed user {} from channel {}", session.getUser().getUsername(), channel.getName());
    }
  }

  private void notifyUserLogout(Session session) {
    // Don't notify if user is restricted
    if (session.getUser().isRestricted()) {
      log.debug(
          "User {} is restricted, skipping logout notification", session.getUser().getUsername());
      return;
    }

    // Notify all other sessions
    sessionService
        .findAll()
        .forEach(
            otherSession -> {
              if (!otherSession.getId().equals(session.getId())) {
                try {
                  ByteArrayOutputStream stream = new ByteArrayOutputStream();
                  packetWriter.writePacket(
                      stream, new UserQuitPacket(session.getUser().getId(), QuitState.GONE));
                  packetBundleService.enqueue(
                      otherSession.getId(), new PacketBundle(stream.toByteArray()));
                } catch (IOException e) {
                  log.error("Error notifying user about logout", e);
                }
              }
            });
  }
}
