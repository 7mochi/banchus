package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.LobbyPartPacket;
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
@HandleClientPacket(Packets.OSU_LOBBY_PART)
public class LobbyPartHandler extends AbstractPacketHandler<LobbyPartPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SessionService sessionService;
  private final ChannelService channelService;

  @Override
  public void handle(LobbyPartPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    session.setReceiveMatchUpdates(false);
    sessionService.update(session);

    channelService
        .findByName("#lobby")
        .ifPresent(
            channel -> {
              channelService.leaveChannel(channel, session);

              Set<UUID> currentChannelMembers = channelService.getMemberIds(channel.getId());

              // TODO: Only get all sessions that has any privilege bit
              for (Session otherOsuSession : sessionService.findAll()) {
                if (!channelService.canRead(channel, otherOsuSession.getUser().getPrivileges()))
                  continue;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                  packetWriter.writePacket(
                      stream,
                      new ChannelAvailablePacket(
                          channel.getName(), channel.getTopic(), currentChannelMembers.size()));
                  packetBundleService.enqueue(
                      otherOsuSession.getId(), new PacketBundle(stream.toByteArray()));
                } catch (IOException e) {
                  log.error("Error sending channel update", e);
                }
              }

              log.info("User {} has part from #lobby.", session.getUser().getUsername());
            });
  }
}
