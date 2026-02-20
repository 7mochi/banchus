package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.MatchPartPacket;
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket;
import pe.nanamochi.banchus.packets.server.MatchDisbandPacket;
import pe.nanamochi.banchus.packets.server.MatchTransferHostPacket;
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
@HandleClientPacket(Packets.OSU_MATCH_PART)
public class MatchPartHandler extends AbstractPacketHandler<MatchPartPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final MultiplayerService multiplayerService;
  private final ChannelService channelService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(MatchPartPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} tried to leave a match while not in a match.", session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match -> {
              try {
                handleMatchPart(session, match);
              } catch (IOException e) {
                log.error("Error handling match part", e);
              }
            },
            () -> {
              log.warn(
                  "User {} tried to leave a match that doesn't exist.",
                  session.getUser().getUsername());
              session.setMultiplayerMatchId(-1);
              sessionService.update(session);
            });
  }

  private void handleMatchPart(Session session, MultiplayerMatch match) throws IOException {
    // Find and reset player's slot
    multiplayerService
        .findSlotBySessionId(match.getMatchId(), session.getId())
        .ifPresentOrElse(
            slot -> {
              // Open up old slot (reset all slot fields)
              MultiplayerMatch updatedMatch =
                  multiplayerService.resetSlot(match.getMatchId(), slot.getSlotId());
              if (updatedMatch != null
                  && updatedMatch.getHostUserId() == session.getUser().getId()) {
                // If the host left, pick a new host or disband match if empty
                try {
                  handleHostLeaving(session, updatedMatch);
                } catch (IOException e) {
                  log.error("Error handling host leaving", e);
                }
              } else if (updatedMatch != null) {
                // Broadcast match updates to all players
                matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());
                log.info(
                    "User {} has left match {}.",
                    session.getUser().getUsername(),
                    match.getMatchId());
              }
            },
            () -> // NOTE: this typically happens when a session is kicked from a match
            log.warn(
                    "User {} attempted to leave their match but they don't have a slot.",
                    session.getUser().getUsername()));

    // Leave the channel
    channelService
        .findByName("#mp_" + match.getMatchId())
        .ifPresent(
            matchChannel -> {
              channelService.leaveChannel(matchChannel, session);
              try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packetWriter.writePacket(stream, new ChannelRevokedPacket("#multiplayer"));
                packetBundleService.enqueue(
                    session.getId(), new PacketBundle(stream.toByteArray()));
              } catch (IOException e) {
                log.error("Error sending channel revoked", e);
              }
            });
  }

  private void handleHostLeaving(Session session, MultiplayerMatch match) throws IOException {
    // Find new host from remaining players
    MultiplayerSlot newHostSlot =
        multiplayerService.getAllSlots(match.getMatchId()).stream()
            .filter(s -> s.getUserId() != -1)
            .findFirst()
            .orElse(null);

    if (newHostSlot == null) {
      // No one is left in the match, close it
      disbandMatch(session, match);
    } else {
      // If the host left, pick a new host
      transferHost(session, match, newHostSlot);
    }
  }

  private void disbandMatch(Session session, MultiplayerMatch match) {
    // Inform everyone in the lobby that the match no longer exists
    channelService
        .findByName("#lobby")
        .ifPresent(
            lobbyChannel ->
                channelService
                    .getMemberIds(lobbyChannel.getId())
                    .forEach(
                        lobbySessionId -> {
                          try {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            packetWriter.writePacket(
                                stream, new MatchDisbandPacket(match.getMatchId()));
                            packetBundleService.enqueue(
                                lobbySessionId, new PacketBundle(stream.toByteArray()));
                          } catch (IOException e) {
                            log.error("Error broadcasting match disband", e);
                          }
                        }));

    // Kick any remaining members from channel (shouldn't be any)
    channelService
        .findByName("#mp_" + match.getMatchId())
        .ifPresent(
            matchChannel -> {
              channelService
                  .getMemberIds(matchChannel.getId())
                  .forEach(
                      memberSessionId ->
                          sessionService
                              .findById(memberSessionId)
                              .ifPresent(
                                  memberSession -> {
                                    try {
                                      ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                      packetWriter.writePacket(
                                          stream, new ChannelRevokedPacket("#multiplayer"));
                                      packetBundleService.enqueue(
                                          memberSessionId, new PacketBundle(stream.toByteArray()));
                                      channelService.leaveChannel(matchChannel, memberSession);
                                    } catch (IOException e) {
                                      log.error("Error kicking member from match channel", e);
                                    }
                                  }));

              channelService.delete(matchChannel); // Delete the multiplayer channel
            });

    multiplayerService.deleteMatch(
        match.getMatchId()); // Delete the multiplayer channel and its slots

    session.setMultiplayerMatchId(-1); // Clear match from session
    sessionService.update(session);

    log.info(
        "Match {} disbanded as the host {} has left and no players remain.",
        match.getMatchId(),
        session.getUser().getUsername());
  }

  private void transferHost(Session session, MultiplayerMatch match, MultiplayerSlot newHostSlot)
      throws IOException {
    match.setHostUserId(newHostSlot.getUserId());
    multiplayerService.update(match);

    // Notify new host
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new MatchTransferHostPacket());
    packetBundleService.enqueue(newHostSlot.getSessionId(), new PacketBundle(stream.toByteArray()));

    // Broadcast updates
    matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());

    log.info(
        "Match {} host {} has left. New host is user ID {}.",
        match.getMatchId(),
        session.getUser().getUsername(),
        newHostSlot.getUserId());
  }
}
