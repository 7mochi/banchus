package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.MatchStatus;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchCompletePacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_COMPLETE)
public class MatchCompleteHandler extends AbstractPacketHandler<MatchCompletePacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public void handle(
      MatchCompletePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} attempted to tell us they completed but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findSlotBySessionId(session.getMultiplayerMatchId(), session.getId())
        .ifPresentOrElse(
            slot -> {
              slot.setStatus(SlotStatus.WAITING_FOR_END.getValue());
              multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);

              boolean allDone =
                  multiplayerService.allPlayersCompleted(session.getMultiplayerMatchId());
              if (!allDone) return;

              multiplayerService
                  .findById(session.getMultiplayerMatchId())
                  .ifPresent(
                      match -> {
                        try {
                          match.setStatus(MatchStatus.WAITING);
                          multiplayerService.update(match);

                          ByteArrayOutputStream stream = new ByteArrayOutputStream();
                          packetWriter.writePacket(
                              stream,
                              new pe.nanamochi.banchus.packets.server.MatchCompletePacket());
                          matchBroadcastService.broadcastToMatch(
                              match, stream.toByteArray(), SlotStatus.COMPLETE.getValue());

                          // Reset all slots for next game
                          for (var s :
                              multiplayerService.getAllSlots(session.getMultiplayerMatchId())) {
                            if (s.getStatus() == SlotStatus.WAITING_FOR_END.getValue()) {
                              s.setStatus(SlotStatus.NOT_READY.getValue());
                              s.setLoaded(false);
                              s.setSkipped(false);
                              multiplayerService.updateSlot(session.getMultiplayerMatchId(), s);
                            }
                          }

                          matchBroadcastService.broadcastMatchUpdates(
                              session.getMultiplayerMatchId(), true, List.of());

                          log.info(
                              "All players in match {} have completed the map.",
                              session.getMultiplayerMatchId());
                        } catch (IOException e) {
                          log.error("Error broadcasting match complete", e);
                        }
                      });
            },
            () ->
                log.warn(
                    "User {} attempted to tell us they completed but they don't have a slot.",
                    session.getUser().getUsername()));
  }
}
