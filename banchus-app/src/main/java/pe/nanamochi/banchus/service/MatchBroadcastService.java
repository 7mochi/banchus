package pe.nanamochi.banchus.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.components.*;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.domain.enums.MatchStatus;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.server.MatchUpdatePacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;
import pe.nanamochi.banchus.redis.model.PacketBundle;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchBroadcastService {
  private final MultiplayerService multiplayerService;
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final ChannelService channelService;

  public void broadcastMatchUpdates(int matchId, boolean sendToLobby, List<UUID> extraSessionIds) {
    multiplayerService
        .findById(matchId)
        .ifPresent(
            match -> {
              try {
                Match matchData = convertToMatchData(match);

                // Send the match data (with password) to those to the multiplayer match
                byte[] updateWithPassword = serializePacket(new MatchUpdatePacket(matchData, true));
                extraSessionIds.forEach(
                    id -> packetBundleService.enqueue(id, new PacketBundle(updateWithPassword)));
                broadcastToMatch(match, updateWithPassword, SlotStatus.HAS_PLAYER.getValue());

                if (sendToLobby) {
                  byte[] updatePublic = serializePacket(new MatchUpdatePacket(matchData, false));
                  broadcastToLobby(updatePublic);
                }
              } catch (IOException e) {
                log.error("Error broadcasting match updates", e);
              }
            });
  }

  public void broadcastToMatch(MultiplayerMatch match, byte[] data, int slotFlags) {
    match.getSlots().stream()
        .filter(slot -> slot.getUserId() != -1 && slot.getSessionId() != null)
        .filter(slot -> (slot.getStatus() & slotFlags) != 0)
        .forEach(slot -> packetBundleService.enqueue(slot.getSessionId(), new PacketBundle(data)));
  }

  public void broadcastToLobby(byte[] data) {
    channelService
        .findByName("#lobby")
        .ifPresentOrElse(
            lobby ->
                channelService
                    .getMemberIds(lobby.getId())
                    .forEach(sid -> packetBundleService.enqueue(sid, new PacketBundle(data))),
            () -> log.warn("Failed to fetch #lobby channel."));
  }

  private byte[] serializePacket(ServerPacket packet) throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, packet);
    return stream.toByteArray();
  }

  public Match convertToMatchData(MultiplayerMatch match) {
    return Match.builder()
        .id(match.getMatchId())
        .inProgress(match.getStatus() == MatchStatus.PLAYING)
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
                            .mods(match.isFreemodsEnabled() ? s.getMods() : 0)
                            .build())
                .toList())
        .build();
  }
}
