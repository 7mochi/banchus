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
import pe.nanamochi.banchus.packets.client.ChannelLeavePacket;
import pe.nanamochi.banchus.packets.server.ChannelAvailablePacket;
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
@HandleClientPacket(value = Packets.OSU_CHANNEL_LEAVE, checkForRestriction = true)
public class ChannelLeaveHandler extends AbstractPacketHandler<ChannelLeavePacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final ChannelService channelService;

  @Override
  public void handle(
      ChannelLeavePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    channelService
        .findByName(packet.getName())
        .ifPresent(
            channel -> {
              try {
                handleChannelLeave(channel, session);
              } catch (IOException e) {
                log.error("Error handling channel leave", e);
              }
            });
  }

  private void handleChannelLeave(Channel channel, Session session) throws IOException {
    // #lobby has its own handler
    if (channel.getName().equals("#lobby") && session.isReceiveMatchUpdates()) {
      return;
    }

    Set<UUID> currentMembers = channelService.getMemberIds(channel.getId());

    if (!currentMembers.contains(session.getId())) {
      log.warn(
          "User {} attempted to leave a channel they are not in.", session.getUser().getUsername());
      return;
    }
    channelService.leaveChannel(channel, session);

    // TODO: Only get all sessions that has any privilege bit
    for (Session otherOsuSession : sessionService.findAll()) {
      if (!channelService.canRead(channel, otherOsuSession.getUser().getPrivileges())) continue;
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      int newMemberCount = !currentMembers.isEmpty() ? currentMembers.size() - 1 : 0;
      packetWriter.writePacket(
          stream,
          new ChannelAvailablePacket(channel.getName(), channel.getTopic(), newMemberCount));
      packetBundleService.enqueue(otherOsuSession.getId(), new PacketBundle(stream.toByteArray()));
    }

    log.info("User {} has left channel {}.", session.getUser().getUsername(), channel.getName());
  }
}
