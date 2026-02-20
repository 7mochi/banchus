package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchFailedPacket;
import pe.nanamochi.banchus.packets.server.MatchPlayerFailedPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_FAILED)
public class MatchFailedHandler extends AbstractPacketHandler<MatchFailedPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public void handle(
      MatchFailedPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} attempted to fail in a match but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findSlotBySessionId(session.getMultiplayerMatchId(), session.getId())
        .ifPresentOrElse(
            slot -> {
              try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packetWriter.writePacket(stream, new MatchPlayerFailedPacket(slot.getSlotId()));
                multiplayerService
                    .findById(session.getMultiplayerMatchId())
                    .ifPresent(
                        match ->
                            matchBroadcastService.broadcastToMatch(
                                match, stream.toByteArray(), SlotStatus.PLAYING.getValue()));
              } catch (IOException e) {
                log.error("Error broadcasting player failed", e);
              }
            },
            () ->
                log.warn(
                    "User {} attempted to fail in a match but they don't have a slot.",
                    session.getUser().getUsername()));
  }
}
