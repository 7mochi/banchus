package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchLoadCompletePacket;
import pe.nanamochi.banchus.packets.server.MatchAllPlayersLoadedPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_LOAD_COMPLETE)
public class MatchLoadCompleteHandler extends AbstractPacketHandler<MatchLoadCompletePacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public void handle(
      MatchLoadCompletePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} attempted to tell us they have loaded but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findSlotBySessionId(session.getMultiplayerMatchId(), session.getId())
        .ifPresentOrElse(
            slot -> {
              slot.setLoaded(true);
              multiplayerService.updateSlot(session.getMultiplayerMatchId(), slot);

              boolean allLoaded =
                  multiplayerService.allPlayersLoaded(session.getMultiplayerMatchId());
              if (!allLoaded) return;

              try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packetWriter.writePacket(stream, new MatchAllPlayersLoadedPacket());
                multiplayerService
                    .findById(session.getMultiplayerMatchId())
                    .ifPresent(
                        match ->
                            matchBroadcastService.broadcastToMatch(
                                match, stream.toByteArray(), SlotStatus.PLAYING.getValue()));
              } catch (IOException e) {
                log.error("Error broadcasting all players loaded", e);
              }
            },
            () ->
                log.warn(
                    "User {} attempted to tell us they have loaded but they don't have a slot.",
                    session.getUser().getUsername()));
  }
}
