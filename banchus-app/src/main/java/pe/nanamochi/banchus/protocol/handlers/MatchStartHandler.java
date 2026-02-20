package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.components.*;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.MatchStatus;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchStartPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_START)
public class MatchStartHandler extends AbstractPacketHandler<MatchStartPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;
  private final PacketWriter packetWriter;

  @Override
  public void handle(MatchStartPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} tried to start a match but they aren't in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match -> {
              if (match.getHostUserId() != session.getUser().getId()) {
                log.warn(
                    "User {} tried to start a match but they aren't the host.",
                    session.getUser().getUsername());
                return;
              }

              match.setStatus(MatchStatus.PLAYING);
              multiplayerService.update(match);

              // Update slots to PLAYING status
              match
                  .getSlots()
                  .forEach(
                      slot -> {
                        if ((slot.getStatus() & SlotStatus.CAN_START.getValue()) != 0) {
                          slot.setStatus(SlotStatus.PLAYING.getValue());
                          multiplayerService.updateSlot(match.getMatchId(), slot);
                        }
                      });

              // Create match data for packet
              Match matchData =
                  Match.builder()
                      .id(match.getMatchId())
                      .inProgress(true)
                      .type(MatchType.STANDARD)
                      .mods(match.getMods())
                      .name(match.getMatchName())
                      .password(match.getMatchPassword())
                      .beatmapName(match.getBeatmapName())
                      .beatmapId(match.getBeatmapId())
                      .beatmapMd5(match.getBeatmapMd5())
                      .hostId(match.getHostUserId())
                      .mode(Mode.fromValue(match.getMode().getValue()))
                      .scoringType(match.getScoringType())
                      .teamType(match.getTeamType())
                      .freemodsEnabled(match.isFreemodsEnabled())
                      .randomSeed(match.getRandomSeed())
                      .slots(
                          match.getSlots().stream()
                              .map(
                                  s ->
                                      MatchSlot.builder()
                                          .userId(s.getUserId())
                                          .status(s.getStatus())
                                          .team(SlotTeam.fromValue(s.getTeam().getValue()))
                                          .mods(s.getMods())
                                          .build())
                              .toList())
                      .build();

              try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packetWriter.writePacket(
                    stream,
                    new pe.nanamochi.banchus.packets.server.MatchStartPacket(matchData, false));

                matchBroadcastService.broadcastToMatch(
                    match, stream.toByteArray(), SlotStatus.PLAYING.getValue());
                matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());
                matchBroadcastService.broadcastToLobby(stream.toByteArray());

                log.info(
                    "User {} started multiplayer match {}.",
                    session.getUser().getUsername(),
                    match.getMatchId());
              } catch (IOException e) {
                log.error("Error broadcasting match start", e);
              }
            },
            () ->
                log.warn(
                    "User {} tried to start a match but their match doesn't exist.",
                    session.getUser().getUsername()));
  }
}
