package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.Mode;
import pe.nanamochi.banchus.domain.enums.Mods;
import pe.nanamochi.banchus.packets.client.UserStatusPacket;
import pe.nanamochi.banchus.packets.server.UserStatsPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.PacketBundleService;
import pe.nanamochi.banchus.service.RankingService;
import pe.nanamochi.banchus.service.SessionService;
import pe.nanamochi.banchus.service.StatService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(value = Packets.OSU_USER_STATUS, checkForRestriction = true)
public class UserStatusHandler extends AbstractPacketHandler<UserStatusPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final StatService statService;
  private final SessionService sessionService;
  private final RankingService rankingService;

  @Override
  public void handle(UserStatusPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    // TODO: Check privileges

    // Convert from packet Mods/Mode to domain Mods/Mode
    Mode gamemode = Mode.fromValue(packet.getMode().getValue());
    int modsBitmask = pe.nanamochi.banchus.components.Mods.toBitmask(packet.getMods());

    // Filter invalid mod combinations, this is a quirk of the osu! client,
    // where it adjusts this value only after it sends the packet to the server,
    // so we need to adjust
    modsBitmask = Mods.filterInvalidModCombinations(modsBitmask, gamemode);

    session.setAction(packet.getAction().getValue());
    session.setInfoText(packet.getText());
    session.setBeatmapMd5(packet.getBeatmapChecksum());
    session.setMods(modsBitmask);
    session.setGamemode(gamemode);
    session.setBeatmapId(packet.getBeatmapId());
    session = sessionService.update(session);

    Session finalSession = session;
    statService
        .findByUserAndGamemode(session.getUser(), gamemode)
        .ifPresent(
            ownStats -> {
              int globalRank =
                  Math.toIntExact(rankingService.getGlobalRank(gamemode, finalSession.getUser()));

              // Send the stats update to all active osu sessions
              for (Session otherSession : sessionService.findAll()) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                  packetWriter.writePacket(
                      stream,
                      new UserStatsPacket(
                          finalSession.getUser().getId(),
                          finalSession.getAction(),
                          finalSession.getInfoText(),
                          finalSession.getBeatmapMd5(),
                          finalSession.getMods(),
                          finalSession.getGamemode(),
                          finalSession.getBeatmapId(),
                          ownStats.getRankedScore(),
                          (float) ownStats.getAccuracy(),
                          ownStats.getPlayCount(),
                          ownStats.getTotalScore(),
                          globalRank,
                          ownStats.getPerformancePoints()));
                  packetBundleService.enqueue(
                      otherSession.getId(), new PacketBundle(stream.toByteArray()));
                } catch (IOException e) {
                  log.error("Error sending user stats update", e);
                }
              }
            });
  }
}
