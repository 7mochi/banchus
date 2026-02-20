package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Channel;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.StopSpectatingPacket;
import pe.nanamochi.banchus.packets.server.*;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.ChannelService;
import pe.nanamochi.banchus.service.PacketBundleService;
import pe.nanamochi.banchus.service.SessionService;
import pe.nanamochi.banchus.service.SpectatorService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(value = Packets.OSU_STOP_SPECTATING, checkForRestriction = true)
public class StopSpectatingHandler extends AbstractPacketHandler<StopSpectatingPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final SpectatorService spectatorService;
  private final ChannelService channelService;

  @Override
  public void handle(
      StopSpectatingPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getUser().isRestricted()) {
      return;
    }

    // Try to find the host session we are spectating
    sessionService
        .findById(session.getSpectatorHostSessionId())
        .ifPresentOrElse(
            hostSession -> {
              try {
                // Remove this session from the host's spectators and clean up
                handleStopSpectating(session, hostSession);
              } catch (IOException e) {
                log.error("Error stopping spectating", e);
              }
            },
            () ->
                log.warn(
                    "The user ({}) attempted to stop spectating another user who is offline.",
                    session.getUser().getId()));
  }

  private void handleStopSpectating(Session session, Session hostSession) throws IOException {
    // Remove this session from the host's spectators
    spectatorService.removeSpectator(hostSession.getId(), session.getId());

    // Clear the spectator host reference in the session
    session.setSpectatorHostSessionId(null);
    Session updatedSession = sessionService.update(session);

    final Session finalSession = updatedSession;
    final Session finalHostSession = hostSession;

    // Clean up the spectator channel
    channelService
        .findByName("#spec_" + hostSession.getId())
        .ifPresent(
            spectatorChannel -> {
              try {
                handleChannelCleanup(spectatorChannel, finalSession, finalHostSession);
              } catch (IOException e) {
                log.error("Error cleaning up spectator channel", e);
              }
            });

    // Notify host that spectator left
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new SpectatorLeftPacket(updatedSession.getUser().getId()));
    packetBundleService.enqueue(hostSession.getId(), new PacketBundle(stream.toByteArray()));

    // Notify other spectators that this user left
    for (UUID spectatorSessionId : spectatorService.getSpectators(hostSession.getId())) {
      if (spectatorSessionId.equals(updatedSession.getId())) {
        continue;
      }

      stream = new ByteArrayOutputStream();
      packetWriter.writePacket(
          stream, new FellowSpectatorLeftPacket(updatedSession.getUser().getId()));
      packetBundleService.enqueue(spectatorSessionId, new PacketBundle(stream.toByteArray()));
    }
  }

  private void handleChannelCleanup(Channel spectatorChannel, Session session, Session hostSession)
      throws IOException {
    // Remove the session from the spectator channel
    channelService.leaveChannel(spectatorChannel, session);

    Set<UUID> currentChannelMemberIds = channelService.getMemberIds(spectatorChannel.getId());
    for (UUID memberSessionId : currentChannelMemberIds) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      packetWriter.writePacket(
          stream,
          new ChannelAvailablePacket(
              "#spectator", spectatorChannel.getTopic(), currentChannelMemberIds.size()));
      packetBundleService.enqueue(memberSessionId, new PacketBundle(stream.toByteArray()));
    }

    // If only the host remains, clean up the channel
    if (currentChannelMemberIds.size() == 1) {
      // Remove the host from the channel
      channelService.leaveChannel(spectatorChannel, hostSession);

      // Delete the channel
      channelService.delete(spectatorChannel);

      // Inform the host that the channel was deleted
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      packetWriter.writePacket(stream, new ChannelRevokedPacket("#spectator"));
      packetBundleService.enqueue(hostSession.getId(), new PacketBundle(stream.toByteArray()));

      log.info("Spectator channel closed due to no spectators.");
    }
  }
}
