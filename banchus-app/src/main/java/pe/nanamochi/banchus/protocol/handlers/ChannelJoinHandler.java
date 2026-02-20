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
import pe.nanamochi.banchus.packets.client.ChannelJoinPacket;
import pe.nanamochi.banchus.packets.server.ChannelAvailablePacket;
import pe.nanamochi.banchus.packets.server.ChannelJoinSuccessPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.ChannelService;
import pe.nanamochi.banchus.service.PacketBundleService;
import pe.nanamochi.banchus.service.SessionService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(value = Packets.OSU_CHANNEL_JOIN, checkForRestriction = true)
public class ChannelJoinHandler extends AbstractPacketHandler<ChannelJoinPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final ChannelService channelService;

  @Override
  public void handle(
      ChannelJoinPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    channelService
        .findByName(packet.getName())
        .ifPresent(
            channel -> {
              try {
                handleChannelJoin(channel, session);
              } catch (IOException e) {
                log.error("Error handling channel join", e);
              }
            });
  }

  private void handleChannelJoin(Channel channel, Session session) throws IOException {
    if (!channelService.canRead(channel, session.getUser().getPrivileges())) return;

    Set<UUID> currentChannelMembers = channelService.getMemberIds(channel.getId());

    if (currentChannelMembers.contains(session.getId())) {
      log.warn(
          "User {} attempted to join a channel they are already in.",
          session.getUser().getUsername());
      return;
    }
    channelService.joinChannel(channel, session);

    ByteArrayOutputStream joinStream = new ByteArrayOutputStream();
    packetWriter.writePacket(joinStream, new ChannelJoinSuccessPacket(channel.getName()));
    packetBundleService.enqueue(session.getId(), new PacketBundle(joinStream.toByteArray()));

    // TODO: Only get all sessions that has any privilege bit
    for (Session otherOsuSession : sessionService.findAll()) {
      if (!channelService.canRead(channel, otherOsuSession.getUser().getPrivileges())) continue;
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      packetWriter.writePacket(
          stream,
          new ChannelAvailablePacket(
              channel.getName(), channel.getTopic(), currentChannelMembers.size() + 1));
      packetBundleService.enqueue(otherOsuSession.getId(), new PacketBundle(stream.toByteArray()));
    }

    log.info("User {} has joined channel {}.", session.getUser().getUsername(), channel.getName());
  }
}
