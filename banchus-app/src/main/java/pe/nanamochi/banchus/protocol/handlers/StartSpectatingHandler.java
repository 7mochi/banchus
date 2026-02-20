package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Channel;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.ServerPrivileges;
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket;
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
@HandleClientPacket(value = Packets.OSU_START_SPECTATING, checkForRestriction = true)
public class StartSpectatingHandler extends AbstractPacketHandler<StartSpectatingPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final SpectatorService spectatorService;
  private final ChannelService channelService;

  @Override
  public void handle(
      StartSpectatingPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getUser().isRestricted()) {
      return;
    }

    sessionService
        .findPrimaryByUserId(packet.getUserId())
        .ifPresentOrElse(
            hostSession -> {
              try {
                handleStartSpectating(packet, session, hostSession);
              } catch (IOException e) {
                log.error("Error starting spectating", e);
              }
            },
            () ->
                log.warn(
                    "The user ({}) attempted to spectate another user ({}) who is offline.",
                    session.getUser().getId(),
                    packet.getUserId()));
  }

  private void handleStartSpectating(
      StartSpectatingPacket packet, Session session, Session hostSession) throws IOException {
    if (packet.getUserId() == session.getUser().getId()) {
      log.warn("Failed to start spectating: Player tried to spectate himself.");
      return;
    }

    if (packet.getUserId() == 1) {
      log.warn("Tried to spectate BanchoBot.");
      return;
    }

    // TODO: tournament client check

    spectatorService.addSpectator(hostSession.getId(), session.getId());

    session.setSpectatorHostSessionId(hostSession.getId());
    Session updatedSession = sessionService.update(session);

    // Make variables final for lambda
    final Session finalSession = updatedSession;
    final Session finalHostSession = hostSession;

    // Fetch the #spectator channel
    channelService
        .findByName("#spec_" + hostSession.getId())
        .ifPresentOrElse(
            spectatorChannel -> {
              try {
                joinExistingSpectatorChannel(spectatorChannel, finalSession, finalHostSession);
              } catch (IOException e) {
                log.error("Error joining existing spectator channel", e);
              }
            },
            () -> {
              try {
                createAndJoinSpectatorChannel(finalSession, finalHostSession);
              } catch (IOException e) {
                log.error("Error creating spectator channel", e);
              }
            });

    // Notify host that spectator joined
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(stream, new SpectatorJoinedPacket(updatedSession.getUser().getId()));
    packetBundleService.enqueue(hostSession.getId(), new PacketBundle(stream.toByteArray()));

    // Notify other spectators only if there are others watching
    if (spectatorService.hasSpectators(hostSession.getId())) {
      for (UUID spectatorSessionId : spectatorService.getSpectators(hostSession.getId())) {
        // Don't notify the person who just joined
        if (spectatorSessionId.equals(updatedSession.getId())) {
          continue;
        }

        ByteArrayOutputStream fellowStream = new ByteArrayOutputStream();
        packetWriter.writePacket(
            fellowStream, new FellowSpectatorJoinedPacket(updatedSession.getUser().getId()));
        packetBundleService.enqueue(
            spectatorSessionId, new PacketBundle(fellowStream.toByteArray()));
      }
    }
  }

  private void createAndJoinSpectatorChannel(Session session, Session hostSession)
      throws IOException {
    Channel spectatorChannel = new Channel();
    spectatorChannel.setName("#spec_" + hostSession.getId());
    spectatorChannel.setTopic("Channel for spectator host ID " + hostSession.getId());
    spectatorChannel.setReadPrivileges(ServerPrivileges.UNRESTRICTED.getValue());
    spectatorChannel.setWritePrivileges(ServerPrivileges.UNRESTRICTED.getValue());
    spectatorChannel.setAutoJoin(false);
    spectatorChannel.setTemporary(true);
    spectatorChannel = channelService.create(spectatorChannel);

    // Add to and inform both host and spectator of the #spectator channel
    for (Session bothSession : List.of(session, hostSession)) {
      channelService.joinChannel(spectatorChannel, bothSession);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      packetWriter.writePacket(
          stream, new ChannelAvailableAutoJoinPacket("#spectator", spectatorChannel.getTopic(), 2));
      packetWriter.writePacket(stream, new ChannelJoinSuccessPacket("#spectator"));
      packetBundleService.enqueue(bothSession.getId(), new PacketBundle(stream.toByteArray()));
    }
  }

  private void joinExistingSpectatorChannel(
      Channel spectatorChannel, Session session, Session hostSession) throws IOException {
    // Join the #spectator channel
    channelService.joinChannel(spectatorChannel, session);

    // Inform everyone in the #spectator channel that we joined
    Set<UUID> currentChannelMembersId = channelService.getMemberIds(spectatorChannel.getId());
    for (UUID memberSessionId : currentChannelMembersId) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      packetWriter.writePacket(
          stream,
          new ChannelAvailablePacket(
              "#spectator", spectatorChannel.getTopic(), currentChannelMembersId.size()));
      packetBundleService.enqueue(memberSessionId, new PacketBundle(stream.toByteArray()));
    }
  }
}
