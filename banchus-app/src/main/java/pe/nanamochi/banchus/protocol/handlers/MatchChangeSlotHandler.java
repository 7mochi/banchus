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
import pe.nanamochi.banchus.packets.client.MatchChangeSlotPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_CHANGE_SLOT)
public class MatchChangeSlotHandler extends AbstractPacketHandler<MatchChangeSlotPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(
      MatchChangeSlotPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} tried to change slot while not in a match.", session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match -> {
              multiplayerService
                  .findSlotBySessionId(match.getMatchId(), session.getId())
                  .ifPresentOrElse(
                      currentSlot -> {
                        // Find target slot using findSlotById
                        multiplayerService
                            .findSlotById(match.getMatchId(), packet.getSlotId())
                            .ifPresentOrElse(
                                targetSlot -> {
                                  if (SlotStatus.fromValue(targetSlot.getStatus())
                                      != SlotStatus.OPEN) {
                                    log.warn(
                                        "User {} tried to change to a slot that is not open: {}.",
                                        session.getUser().getUsername(),
                                        packet.getSlotId());
                                    return;
                                  }

                                  // Switch to new slot
                                  targetSlot.setUserId(currentSlot.getUserId());
                                  targetSlot.setSessionId(currentSlot.getSessionId());
                                  targetSlot.setStatus(currentSlot.getStatus());
                                  targetSlot.setTeam(currentSlot.getTeam());
                                  targetSlot.setMods(currentSlot.getMods());
                                  targetSlot.setLoaded(currentSlot.isLoaded());
                                  targetSlot.setSkipped(currentSlot.isSkipped());
                                  multiplayerService.updateSlot(match.getMatchId(), targetSlot);

                                  // Open up old slot
                                  multiplayerService.resetSlot(
                                      match.getMatchId(), currentSlot.getSlotId());

                                  // Send updated data to those in the multi match, and #lobby
                                  matchBroadcastService.broadcastMatchUpdates(
                                      match.getMatchId(), true, List.of());

                                  log.info(
                                      "User {} switched from slot {} to slot {} in match {}.",
                                      session.getUser().getUsername(),
                                      currentSlot.getSlotId(),
                                      targetSlot.getSlotId(),
                                      match.getMatchId());
                                },
                                () ->
                                    log.warn(
                                        "User {} tried to change to a slot that doesn't exist: {}.",
                                        session.getUser().getUsername(),
                                        packet.getSlotId()));
                      },
                      () ->
                          log.warn(
                              "User {} not inside of a slot.", session.getUser().getUsername()));
            },
            () ->
                log.warn(
                    "User {} tried to change slot but their match doesn't exist.",
                    session.getUser().getUsername()));
  }
}
