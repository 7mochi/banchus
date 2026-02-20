package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.MatchChangePasswordPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_CHANGE_PASSWORD)
public class MatchChangePasswordHandler extends AbstractPacketHandler<MatchChangePasswordPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(
      MatchChangePasswordPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} attempted to change match password but is not in a match",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match -> {
              if (match.getHostUserId() != session.getUser().getId()) {
                log.warn(
                    "User {} attempted to change the match password but is not the host",
                    session.getUser().getUsername());
                return;
              }

              match.setMatchPassword(packet.getMatch().getPassword());
              multiplayerService.update(match);

              matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());

              log.info(
                  "User {} updated the match password for match {}",
                  session.getUser().getUsername(),
                  session.getMultiplayerMatchId());
            },
            () ->
                log.warn(
                    "User {} attempted to change the match password but their match doesn't exist",
                    session.getUser().getUsername()));
  }
}
