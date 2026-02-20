package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.components.Match;
import pe.nanamochi.banchus.components.MatchSlot;
import pe.nanamochi.banchus.components.MatchType;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.MatchStatus;
import pe.nanamochi.banchus.packets.client.LobbyJoinPacket;
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;
import pe.nanamochi.banchus.redis.model.MultiplayerSlot;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.MultiplayerService;
import pe.nanamochi.banchus.service.PacketBundleService;
import pe.nanamochi.banchus.service.SessionService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_LOBBY_JOIN)
public class LobbyJoinHandler extends AbstractPacketHandler<LobbyJoinPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final MultiplayerService multiplayerService;

  @Override
  public void handle(LobbyJoinPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    session.setReceiveMatchUpdates(true);
    session = sessionService.update(session);

    List<MultiplayerMatch> matches = multiplayerService.findAll();

    for (MultiplayerMatch match : matches) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();

      List<MatchSlot> matchSlotsData = new ArrayList<>();
      for (MultiplayerSlot slot : match.getSlots()) {
        matchSlotsData.add(
            MatchSlot.builder()
                .userId(slot.getUserId())
                .status(slot.getStatus())
                .team(pe.nanamochi.banchus.components.SlotTeam.fromValue(slot.getTeam().getValue()))
                .mods(slot.getMods())
                .build());
      }

      Match matchData =
          Match.builder()
              .id(match.getMatchId())
              .inProgress(match.getStatus().getValue() == MatchStatus.PLAYING.getValue())
              .type(MatchType.STANDARD)
              .mods(match.getMods())
              .name(match.getMatchName())
              .password(match.getMatchPassword())
              .beatmapName(match.getBeatmapName())
              .beatmapId(match.getBeatmapId())
              .beatmapMd5(match.getBeatmapMd5())
              .slots(matchSlotsData)
              .hostId(match.getHostUserId())
              .mode(pe.nanamochi.banchus.components.Mode.fromValue(match.getMode().getValue()))
              .scoringType(match.getScoringType())
              .teamType(match.getTeamType())
              .freemodsEnabled(match.isFreemodsEnabled())
              .randomSeed(match.getRandomSeed())
              .build();

      packetWriter.writePacket(stream, new MatchUpdatePacket(matchData, false));

      packetBundleService.enqueue(session.getId(), new PacketBundle(stream.toByteArray()));
    }
  }
}
