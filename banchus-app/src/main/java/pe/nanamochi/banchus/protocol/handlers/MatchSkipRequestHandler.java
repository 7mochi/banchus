package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchSkipRequestPacket;
import pe.nanamochi.banchus.packets.server.MatchPlayerSkippedPacket;
import pe.nanamochi.banchus.packets.server.MatchSkipPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_SKIP_REQUEST)
public class MatchSkipRequestHandler extends AbstractPacketHandler<MatchSkipRequestPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public void handle(
      MatchSkipRequestPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} attempted to request a skip but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findSlotBySessionId(session.getMultiplayerMatchId(), session.getId())
        .ifPresentOrElse(
            slot -> {
              slot.setSkipped(true);
              multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);

              try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packetWriter.writePacket(stream, new MatchPlayerSkippedPacket(slot.getSlotId()));

                boolean allSkipped =
                    multiplayerService.allPlayersSkipped(session.getMultiplayerMatchId());
                if (allSkipped) {
                  packetWriter.writePacket(stream, new MatchSkipPacket());
                }

                multiplayerService
                    .findById(session.getMultiplayerMatchId())
                    .ifPresent(
                        match ->
                            matchBroadcastService.broadcastToMatch(
                                match, stream.toByteArray(), SlotStatus.PLAYING.getValue()));
              } catch (IOException e) {
                log.error("Error broadcasting skip request", e);
              }
            },
            () ->
                log.warn(
                    "User {} attempted to skip but they don't have a slot.",
                    session.getUser().getUsername()));
  }
}
