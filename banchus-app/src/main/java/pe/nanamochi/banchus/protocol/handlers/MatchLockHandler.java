package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.domain.enums.SlotTeam;
import pe.nanamochi.banchus.packets.client.MatchLockPacket;
import pe.nanamochi.banchus.packets.server.ChannelRevokedPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.ChannelService;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;
import pe.nanamochi.banchus.service.PacketBundleService;
import pe.nanamochi.banchus.service.SessionService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_LOCK)
public class MatchLockHandler extends AbstractPacketHandler<MatchLockPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final SessionService sessionService;
  private final ChannelService channelService;
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;

  @Override
  public void handle(MatchLockPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} tried to (un)lock a slot but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match -> {
              // Only the host can edit slots
              if (match.getHostUserId() != session.getUser().getId()) {
                log.warn(
                    "User {} tried to (un)lock a slot but they are not the host.",
                    session.getUser().getUsername());
                return;
              }

              multiplayerService
                  .findSlotById(match.getMatchId(), packet.getSlotId())
                  .ifPresentOrElse(
                      slot -> {
                        // If the slot is occupied, kick the player
                        if (slot.getUserId() != -1) {
                          if (slot.getUserId() == session.getUser().getId()) {
                            log.warn(
                                "User {} tried to (un)lock a slot that they are currently"
                                    + " occupying: {}.",
                                session.getUser().getUsername(),
                                packet.getSlotId());
                            return;
                          }

                          // Find the session of the player to be kicked
                          sessionService
                              .findPrimaryByUserId(slot.getUserId())
                              .ifPresent(
                                  slotSession -> {
                                    // Remove the player from the match channel
                                    channelService
                                        .findByName("#mp_" + match.getMatchId())
                                        .ifPresent(
                                            matchChannel -> {
                                              channelService.leaveChannel(
                                                  matchChannel, slotSession);

                                              try {
                                                ByteArrayOutputStream stream =
                                                    new ByteArrayOutputStream();
                                                packetWriter.writePacket(
                                                    stream,
                                                    new ChannelRevokedPacket("#multiplayer"));
                                                packetBundleService.enqueue(
                                                    slotSession.getId(),
                                                    new PacketBundle(stream.toByteArray()));
                                              } catch (IOException e) {
                                                log.error("Error sending kick notification", e);
                                              }

                                              log.info(
                                                  "User {} was kicked from match {} by host {}.",
                                                  slotSession.getUser().getUsername(),
                                                  match.getMatchId(),
                                                  session.getUser().getUsername());
                                            });
                                  });
                        }

                        // Toggle lock status
                        // If currently locked, unlock; otherwise, lock
                        SlotStatus newStatus =
                            SlotStatus.fromValue(slot.getStatus()) == SlotStatus.LOCKED
                                ? SlotStatus.OPEN
                                : SlotStatus.LOCKED;

                        // Reset and lock/unlock slot
                        slot.setUserId(-1);
                        slot.setSessionId(null);
                        slot.setStatus(newStatus.getValue());
                        slot.setTeam(SlotTeam.NEUTRAL);
                        slot.setMods(0);
                        slot.setLoaded(false);
                        slot.setSkipped(false);
                        multiplayerService.updateSlot(match.getMatchId(), slot);

                        // Broadcast updates to all players
                        matchBroadcastService.broadcastMatchUpdates(
                            match.getMatchId(), true, List.of());

                        log.info(
                            "User {} {} slot {} in match {}.",
                            session.getUser().getUsername(),
                            newStatus == SlotStatus.LOCKED ? "locked" : "unlocked",
                            packet.getSlotId(),
                            match.getMatchId());
                      },
                      () ->
                          log.warn(
                              "User {} tried to (un)lock a slot that doesn't exist: {}.",
                              session.getUser().getUsername(),
                              packet.getSlotId()));
            },
            () ->
                log.warn(
                    "User {} tried to (un)lock a slot but their match doesn't exist.",
                    session.getUser().getUsername()));
  }
}
