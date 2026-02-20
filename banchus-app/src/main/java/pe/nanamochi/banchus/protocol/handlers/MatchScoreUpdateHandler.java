package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchScoreUpdatePacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_SCORE_UPDATE)
public class MatchScoreUpdateHandler extends AbstractPacketHandler<MatchScoreUpdatePacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public void handle(
      MatchScoreUpdatePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} sent a match score frame but they are not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match ->
                multiplayerService
                    .findSlotBySessionId(match.getMatchId(), session.getId())
                    .ifPresentOrElse(
                        slot -> {
                          packet.getFrame().setId(slot.getSlotId());

                          try {
                            ByteArrayOutputStream packetStream = new ByteArrayOutputStream();
                            packetWriter.writePacket(
                                packetStream,
                                new pe.nanamochi.banchus.packets.server.MatchScoreUpdatePacket(
                                    packet.getFrame()));

                            matchBroadcastService.broadcastToMatch(
                                match, packetStream.toByteArray(), SlotStatus.PLAYING.getValue());
                          } catch (IOException e) {
                            log.error("Error broadcasting score update", e);
                          }
                        },
                        () ->
                            log.warn(
                                "User {} sent a match score frame but they are not in a slot.",
                                session.getUser().getUsername())),
            () ->
                log.warn(
                    "User {} sent a match score frame but their match does not exist.",
                    session.getUser().getUsername()));
  }
}
