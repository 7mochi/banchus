package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.Mods;
import pe.nanamochi.banchus.packets.client.MatchChangeModsPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;
import pe.nanamochi.banchus.service.SessionService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_CHANGE_MODS)
public class MatchChangeModsHandler extends AbstractPacketHandler<MatchChangeModsPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final SessionService sessionService;

  @Override
  public void handle(
      MatchChangeModsPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} tried to change match mods while not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match -> {
              boolean isHost = match.getHostUserId() == session.getUser().getId();

              // TODO: convert mode for client and server?

              if (match.isFreemodsEnabled()) {
                // In freemod mode, split mods between match (speed-changing) and slot
                // (non-speed-changing)
                if (isHost) {
                  // Apply the speed changing mods to the match
                  match.setMods(packet.getMods() & Mods.SPEED_CHANGING);
                  multiplayerService.update(match);
                }

                // And apply the non-speed changing mods to the slot
                multiplayerService
                    .findSlotBySessionId(session.getMultiplayerMatchId(), session.getId())
                    .ifPresentOrElse(
                        slot -> {
                          slot.setMods(packet.getMods() & ~Mods.SPEED_CHANGING);
                          multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);

                          // Set the session's mode if needed
                          if (session.getGamemode() != match.getMode()) {
                            session.setGamemode(match.getMode());
                            sessionService.update(session);
                          }
                        },
                        () ->
                            log.warn(
                                "User {} tried to change mods but their slot doesn't exist.",
                                session.getUser().getUsername()));
                matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());
              } else if (isHost) {
                // In non-freemod mode, only host can change mods and applies to all
                match.setMods(packet.getMods());
                multiplayerService.update(match);

                // Set all sessions' mode if needed
                if (session.getGamemode() != match.getMode()) {
                  multiplayerService
                      .getAllSlots(session.getMultiplayerMatchId())
                      .forEach(
                          slot -> {
                            // Only update valid slots (not empty or bot)
                            if (slot.getUserId() != -1 && slot.getUserId() != 1) {
                              sessionService
                                  .findById(slot.getSessionId())
                                  .ifPresent(
                                      slotSession -> {
                                        slotSession.setGamemode(match.getMode());
                                        slotSession.setMods(packet.getMods());
                                        sessionService.update(slotSession);
                                      });
                            }
                          });
                }
                matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());
              } else {
                log.warn(
                    "User {} attempted to change the match mods but they aren't allowed to.",
                    session.getUser().getUsername());
                return;
              }

              log.info(
                  "User {} changed the match mods to {}.",
                  session.getUser().getUsername(),
                  Mods.fromBitmask(packet.getMods()));
            },
            () ->
                log.warn(
                    "User {} tried to change mods but their match doesn't exist.",
                    session.getUser().getUsername()));
  }
}
