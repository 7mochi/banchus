package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.StatusUpdateRequestPacket;
import pe.nanamochi.banchus.packets.server.UserStatsPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.PacketBundleService;
import pe.nanamochi.banchus.service.RankingService;
import pe.nanamochi.banchus.service.StatService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(value = Packets.OSU_STATUS_UPDATE_REQUEST, checkForRestriction = true)
public class StatusUpdateRequestHandler extends AbstractPacketHandler<StatusUpdateRequestPacket> {
  private final StatService statService;
  private final RankingService rankingService;
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;

  @Override
  public void handle(
      StatusUpdateRequestPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    statService
        .findByUserAndGamemode(session.getUser(), session.getGamemode())
        .ifPresent(
            ownStats -> {
              int ownGlobalRank =
                  Math.toIntExact(
                      rankingService.getGlobalRank(session.getGamemode(), session.getUser()));

              ByteArrayOutputStream stream = new ByteArrayOutputStream();
              try {
                packetWriter.writePacket(
                    stream,
                    new UserStatsPacket(
                        session.getUser().getId(),
                        session.getAction(),
                        session.getInfoText(),
                        session.getBeatmapMd5(),
                        session.getMods(),
                        session.getGamemode(),
                        session.getBeatmapId(),
                        ownStats.getRankedScore(),
                        (float) ownStats.getAccuracy(),
                        ownStats.getPlayCount(),
                        ownStats.getTotalScore(),
                        ownGlobalRank,
                        ownStats.getPerformancePoints()));
                packetBundleService.enqueue(
                    session.getId(), new PacketBundle(stream.toByteArray()));
              } catch (IOException e) {
                log.error("Error writing status update response", e);
              }
            });
  }
}
