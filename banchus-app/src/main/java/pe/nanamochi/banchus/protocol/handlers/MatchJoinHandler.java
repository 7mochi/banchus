package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchJoinPacket;
import pe.nanamochi.banchus.packets.server.*;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.*;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_JOIN)
public class MatchJoinHandler extends AbstractPacketHandler<MatchJoinPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final MultiplayerService multiplayerService;
  private final ChannelService channelService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(MatchJoinPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    // Validate user restrictions before attempting to join
    if (session.getUser().isRestricted()) {
      sendFailure(session, "Multiplayer is not available while restricted.");
      log.warn(
          "A restricted user ({}) attempted to join a multiplayer match.",
          session.getUser().getUsername());
      return;
    }

    if (session.getUser().isSilenced()) {
      sendFailure(session, "Multiplayer is not available while silenced.");
      log.warn(
          "A silenced user ({}) attempted to join a multiplayer match.",
          session.getUser().getUsername());
      return;
    }

    // Attempt to find the match we are trying to join
    multiplayerService
        .findById(packet.getMatchId())
        .ifPresentOrElse(
            match -> {
              try {
                handleJoinMatch(packet, session, match);
              } catch (IOException e) {
                log.error("Error handling match join", e);
              }
            },
            () -> {
              log.warn(
                  "User {} tried to join a non-existing match with ID {}",
                  session.getUser().getUsername(),
                  packet.getMatchId());
              try {
                sendFailure(session, null);
              } catch (IOException e) {
                log.error("Error sending failure", e);
              }
            });
  }

  private void handleJoinMatch(MatchJoinPacket packet, Session session, MultiplayerMatch match)
      throws IOException {
    // If the match has a non-empty password, validate the client got it right
    if (!match.getMatchPassword().isEmpty()
        && !match.getMatchPassword().equals(packet.getMatchPassword())) {
      log.warn(
          "User {} tried to join a match with an incorrect password",
          session.getUser().getUsername());
      sendFailure(session, null);
      return;
    }

    // Claim a slot for the session
    multiplayerService
        .claimFirstAvailableSlotId(match.getMatchId())
        .ifPresentOrElse(
            slotId -> {
              try {
                joinMatchAndSlot(session, match, slotId);
              } catch (IOException e) {
                log.error("Error joining match and slot", e);
              }
            },
            () -> {
              log.error("Failed to claim slot ID for multiplayer match ID {}", match.getMatchId());
              try {
                sendFailure(session, null);
              } catch (IOException e) {
                log.error("Error sending failure", e);
              }
            });
  }

  private void joinMatchAndSlot(Session session, MultiplayerMatch match, int slotId)
      throws IOException {
    // Update slot with user and session info, set status to NOT_READY
    multiplayerService
        .findSlotById(match.getMatchId(), slotId)
        .ifPresent(
            slot -> {
              slot.setUserId(session.getUser().getId());
              slot.setSessionId(session.getId());
              slot.setStatus(SlotStatus.NOT_READY.getValue());
              multiplayerService.updateSlot(match.getMatchId(), slot);
            });

    // Join the multiplayer match and update session
    session.setMultiplayerMatchId(match.getMatchId());
    sessionService.update(session);

    // Join the #multiplayer channel
    channelService
        .findByName("#mp_" + match.getMatchId())
        .ifPresent(
            matchChannel -> {
              channelService.joinChannel(matchChannel, session);
              int memberCount = channelService.getMemberIds(matchChannel.getId()).size();

              try {
                // Inform our user of the #multiplayer channel
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                packetWriter.writePacket(
                    stream,
                    new ChannelAvailableAutoJoinPacket(
                        "#multiplayer", matchChannel.getTopic(), memberCount));
                packetWriter.writePacket(stream, new ChannelJoinSuccessPacket("#multiplayer"));

                // Send the match data (with password) to the user
                Match matchData = buildMatchData(match);
                packetWriter.writePacket(stream, new MatchJoinSuccessPacket(matchData, true));
                packetBundleService.enqueue(
                    session.getId(), new PacketBundle(stream.toByteArray()));

                // Broadcast match updates to all players
                matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());

                log.info(
                    "User {} joined match {}", session.getUser().getUsername(), match.getMatchId());
              } catch (IOException e) {
                log.error("Error sending join success packets", e);
              }
            });

    log.info("User {} joined match {}", session.getUser().getUsername(), match.getMatchId());
  }

  private void sendFailure(Session session, String message) throws IOException {
    // Send failure packet and optional announce message to the user
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new MatchJoinFailPacket());
    if (message != null) {
      packetWriter.writePacket(stream, new AnnouncePacket(message));
    }
    packetBundleService.enqueue(session.getId(), new PacketBundle(stream.toByteArray()));
  }

  private Match buildMatchData(MultiplayerMatch match) {
    // Build the Match data object to send to the client, including all slots
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
        .mode(pe.nanamochi.banchus.components.Mode.fromValue(match.getMode().getValue()))
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
                            .team(
                                pe.nanamochi.banchus.components.SlotTeam.fromValue(
                                    s.getTeam().getValue()))
                            .mods(s.getMods())
                            .build())
                .toList())
        .build();
  }
}
