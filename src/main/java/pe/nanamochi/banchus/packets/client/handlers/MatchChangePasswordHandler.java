package pe.nanamochi.banchus.packets.client.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.entities.db.Session;
import pe.nanamochi.banchus.entities.redis.MultiplayerMatch;
import pe.nanamochi.banchus.packets.AbstractPacketHandler;
import pe.nanamochi.banchus.packets.Packets;
import pe.nanamochi.banchus.packets.client.MatchChangePasswordPacket;
import pe.nanamochi.banchus.services.*;

@Component
@RequiredArgsConstructor
public class MatchChangePasswordHandler extends AbstractPacketHandler<MatchChangePasswordPacket> {
  private static final Logger logger = LoggerFactory.getLogger(MatchChangePasswordHandler.class);

  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public Packets getPacketType() {
    return Packets.OSU_MATCH_CHANGE_PASSWORD;
  }

  @Override
  public Class<MatchChangePasswordPacket> getPacketClass() {
    return MatchChangePasswordPacket.class;
  }

  @Override
  public void handle(
      MatchChangePasswordPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    logger.debug("Handling packet: {}", getPacketType());

    if (session.getMultiplayerMatchId() == null) {
      logger.warn(
          "User {} attempted to change match password but is not in a match",
          session.getUser().getUsername());
      return;
    }

    MultiplayerMatch match = multiplayerService.findById(session.getMultiplayerMatchId());
    if (match == null) {
      logger.warn(
          "User {} attempted to change the match password but their match doesn't exist",
          session.getUser().getUsername());
      return;
    }

    if (match.getHostUserId() != session.getUser().getId()) {
      logger.warn(
          "User {} attempted to change the match password but is not the host",
          session.getUser().getUsername());
      return;
    }

    match.setMatchPassword(packet.getMatch().getPassword());
    multiplayerService.update(match);

    matchBroadcastService.broadcastMatchUpdates(session.getMultiplayerMatchId(), true, List.of());

    logger.info(
        "User {} updated the match password for match {}",
        session.getUser().getUsername(),
        session.getMultiplayerMatchId());
  }
}
