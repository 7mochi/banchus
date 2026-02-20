package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.components.Match;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.database.entity.Channel;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.MatchStatus;
import pe.nanamochi.banchus.domain.enums.ServerPrivileges;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.packets.client.MatchCreatePacket;
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
@HandleClientPacket(Packets.OSU_MATCH_CREATE)
public class MatchCreateHandler extends AbstractPacketHandler<MatchCreatePacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final MultiplayerService multiplayerService;
  private final SpectatorService spectatorService;
  private final ChannelService channelService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(
      MatchCreatePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    // If the user is restricted or silenced, send failure and log
    if (session.getUser().isRestricted() || session.getUser().isSilenced()) {
      // Send failure packets and log the reason
      String reason = session.getUser().isRestricted() ? "restricted" : "silenced";
      sendFailureMessage(session, "Multiplayer is not available while " + reason + ".");
      log.warn(
          "A {} user ({}) attempted to create a multiplayer match.",
          reason,
          session.getUser().getUsername());
      return;
    }

    // If we are spectating someone, stop spectating them first
    Optional.ofNullable(session.getSpectatorHostSessionId())
        .ifPresent(_ -> stopSpectating(session));

    // Create the multiplayer match in Redis
    MultiplayerMatch match =
        MultiplayerMatch.builder()
            .matchName(packet.getMatch().getName())
            .matchPassword(packet.getMatch().getPassword())
            .beatmapName(packet.getMatch().getBeatmapName())
            .beatmapId(packet.getMatch().getBeatmapId())
            .beatmapMd5(packet.getMatch().getBeatmapMd5())
            .hostUserId(session.getUser().getId())
            .mode(
                pe.nanamochi.banchus.domain.enums.Mode.fromValue(
                    packet.getMatch().getMode().getValue()))
            .mods(packet.getMatch().getMods())
            .scoringType(packet.getMatch().getScoringType())
            .teamType(packet.getMatch().getTeamType())
            .freemodsEnabled(packet.getMatch().isFreemodsEnabled())
            .randomSeed(packet.getMatch().getRandomSeed())
            .status(MatchStatus.WAITING)
            .build();

    MultiplayerMatch savedMatch = multiplayerService.create(match);

    // Create the multiplayer chat channel (#mp_ID)
    Channel matchChannel = createMultiplayerChannel(savedMatch.getMatchId());

    // Try to occupy the first slot (usually ID 0)
    multiplayerService
        .claimFirstAvailableSlotId(savedMatch.getMatchId())
        .ifPresentOrElse(
            slotId -> joinMatchAndSlot(session, savedMatch, matchChannel, slotId),
            () -> sendFailureMessage(session, "Failed to initialize match slots."));
  }

  private Channel createMultiplayerChannel(int matchId) {
    // Create a temporary channel for the multiplayer match
    Channel channel = new Channel();
    channel.setName("#mp_" + matchId);
    channel.setTopic("Multiplayer match " + matchId);
    channel.setReadPrivileges(ServerPrivileges.UNRESTRICTED.getValue());
    channel.setWritePrivileges(ServerPrivileges.UNRESTRICTED.getValue());
    channel.setAutoJoin(false);
    channel.setTemporary(true);
    return channelService.create(channel);
  }

  private void joinMatchAndSlot(
      Session session, MultiplayerMatch match, Channel channel, int slotId) {
    // Assign the user to the claimed slot
    multiplayerService
        .findSlotById(match.getMatchId(), slotId)
        .ifPresent(
            slot -> {
              slot.setUserId(session.getUser().getId());
              slot.setSessionId(session.getId());
              slot.setStatus(SlotStatus.NOT_READY.getValue());
              multiplayerService.updateSlot(match.getMatchId(), slot);
            });

    // Set the multiplayer match ID in the session
    session.setMultiplayerMatchId(match.getMatchId());
    sessionService.update(session);

    // Join the #multiplayer channel
    channelService.joinChannel(channel, session);

    // Get the updated match after assigning the slot
    multiplayerService
        .findById(match.getMatchId())
        .ifPresent(
            updatedMatch -> {
              try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                // Inform the user of the #multiplayer channel
                packetWriter.writePacket(
                    stream,
                    new ChannelAvailableAutoJoinPacket("#multiplayer", channel.getTopic(), 1));
                packetWriter.writePacket(stream, new ChannelJoinSuccessPacket("#multiplayer"));

                // Send the match data (with password) to the creator
                Match matchData = matchBroadcastService.convertToMatchData(updatedMatch);
                packetWriter.writePacket(stream, new MatchJoinSuccessPacket(matchData, true));

                packetBundleService.enqueue(
                    session.getId(), new PacketBundle(stream.toByteArray()));

                // Broadcast match updates to all players
                matchBroadcastService.broadcastMatchUpdates(
                    match.getMatchId(), true, java.util.List.of());

                log.info(
                    "User {} created match {} [{}]",
                    session.getUser().getUsername(),
                    match.getMatchId(),
                    match.getMatchName());
              } catch (IOException e) {
                log.error("Error joinMatchAndSlot for match {}", match.getMatchId(), e);
              }
            });
  }

  private void stopSpectating(Session session) {
    // If we are spectating someone, remove us from their spectators and notify all relevant parties
    sessionService
        .findById(session.getSpectatorHostSessionId())
        .ifPresent(
            host -> {
              spectatorService.removeSpectator(host.getId(), session.getId());
              session.setSpectatorHostSessionId(null);
              sessionService.update(session);

              try {
                // Inform the host that we left
                byte[] leftData = serialize(new SpectatorLeftPacket(session.getUser().getId()));
                packetBundleService.enqueue(host.getId(), new PacketBundle(leftData));

                // Inform the other spectators that we left
                byte[] fellowLeftData =
                    serialize(new FellowSpectatorLeftPacket(session.getUser().getId()));
                spectatorService
                    .getSpectators(host.getId())
                    .forEach(
                        sid -> {
                          if (!sid.equals(session.getId())) {
                            packetBundleService.enqueue(sid, new PacketBundle(fellowLeftData));
                          }
                        });
              } catch (IOException e) {
                log.error("Error stopSpectating", e);
              }
            });
  }

  private byte[] serialize(ServerPacket packet) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    packetWriter.writePacket(os, packet);
    return os.toByteArray();
  }

  private void sendFailureMessage(Session session, String msg) {
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      packetWriter.writePacket(os, new MatchJoinFailPacket());
      packetWriter.writePacket(os, new AnnouncePacket(msg));
      packetBundleService.enqueue(session.getId(), new PacketBundle(os.toByteArray()));
    } catch (IOException e) {
      log.error("Error sending MatchJoinFail", e);
    }
  }
}
