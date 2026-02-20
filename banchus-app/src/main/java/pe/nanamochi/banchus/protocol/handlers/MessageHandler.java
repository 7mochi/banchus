package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.commands.CommandProcessor;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Channel;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.MessagePacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.ChannelService;
import pe.nanamochi.banchus.service.PacketBundleService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(value = Packets.OSU_MESSAGE, checkForRestriction = true)
public class MessageHandler extends AbstractPacketHandler<MessagePacket> {
  @Value("${banchus.command-prefix:!}")
  private String commandPrefix;

  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final ChannelService channelService;
  private final CommandProcessor commandProcessor;

  @Override
  public void handle(MessagePacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    // Check if user is silenced
    if (session.getUser().isSilenced()) {
      log.warn(
          "User {} attempted to send a message while silenced.", session.getUser().getUsername());
      return;
    }

    // Resolve channel name (handle #multiplayer and #spectator)
    String channelName = resolveChannelName(packet, session);
    if (channelName == null) {
      log.warn(
          "User {} attempted to send a message to an invalid channel.",
          session.getUser().getUsername());
      return;
    }

    // Find channel
    channelService
        .findByName(channelName)
        .ifPresentOrElse(
            channel -> {
              try {
                handleMessage(packet, session, channel);
              } catch (IOException e) {
                log.error("Error handling message", e);
              }
            },
            () ->
                log.warn(
                    "User {} attempted to send a message to non-existent channel {}.",
                    session.getUser().getUsername(),
                    channelName));
  }

  private void handleMessage(MessagePacket packet, Session session, Channel channel)
      throws IOException {
    // Check write permissions
    if (!channelService.canWrite(channel, session.getUser().getPrivileges())) {
      log.warn(
          "User {} attempted to send a message to channel {} without sufficient privileges.",
          session.getUser().getUsername(),
          channel.getName());
      return;
    }

    // Truncate message if too long
    if (packet.getContent().length() > 2000) {
      packet.setContent(packet.getContent().substring(0, 2000) + "...");
      log.debug(
          "Truncated message from user {} (exceeded 2000 chars)", session.getUser().getUsername());
    }

    // Check if it's a private command (like !help)
    boolean isPrivateCommand = packet.getContent().startsWith(commandPrefix + "help");

    // Broadcast message to channel (unless it's a private command)
    if (!isPrivateCommand) {
      broadcastToChannel(packet, session, channel);
    }

    // Handle commands
    handleCommands(session, packet, channel, isPrivateCommand);
  }

  private String resolveChannelName(MessagePacket packet, Session session) {
    String target = packet.getTarget();

    // Handle #multiplayer alias
    if (target.equals("#multiplayer")) {
      if (session.getMultiplayerMatchId() != null) {
        return "#mp_" + session.getMultiplayerMatchId();
      }
      log.debug(
          "User {} tried to send message to #multiplayer but is not in a match",
          session.getUser().getUsername());
      return null;
    }

    // Handle #spectator alias
    if (target.equals("#spectator")) {
      UUID hostId =
          (session.getSpectatorHostSessionId() != null)
              ? session.getSpectatorHostSessionId()
              : session.getId();
      return "#spec_" + hostId;
    }

    // Regular channel
    return target;
  }

  private void broadcastToChannel(MessagePacket packet, Session session, Channel channel)
      throws IOException {
    byte[] msgBytes =
        createMessagePacket(
            session.getUser().getUsername(),
            packet.getContent(),
            packet.getTarget(),
            session.getUser().getId());

    Set<UUID> members = channelService.getMemberIds(channel.getId());
    members.stream()
        .filter(targetId -> !targetId.equals(session.getId()))
        .forEach(targetId -> packetBundleService.enqueue(targetId, new PacketBundle(msgBytes)));

    log.debug(
        "User {} sent message to channel {} ({} members)",
        session.getUser().getUsername(),
        channel.getName(),
        members.size());
  }

  private byte[] createMessagePacket(String sender, String content, String target, int userId)
      throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    packetWriter.writePacket(
        stream,
        new pe.nanamochi.banchus.packets.server.MessagePacket(sender, content, target, userId));
    return stream.toByteArray();
  }

  private void handleCommands(
      Session session, MessagePacket packet, Channel channel, boolean isPrivateCommand)
      throws IOException {
    // Process command
    String result =
        commandProcessor.handle(commandPrefix, packet.getContent(), session.getUser(), channel);

    if (result == null || result.trim().isEmpty()) {
      return;
    }

    // Determine target sessions (private or channel-wide)
    Set<UUID> targetSessions =
        isPrivateCommand ? Set.of(session.getId()) : channelService.getMemberIds(channel.getId());

    // Send BanchoBot response
    byte[] responsePacket = createMessagePacket("BanchoBot", result, packet.getTarget(), 1);
    targetSessions.forEach(
        targetSessionId ->
            packetBundleService.enqueue(targetSessionId, new PacketBundle(responsePacket)));

    log.debug(
        "Command '{}' executed by {} in channel {} -> {} recipients",
        packet.getContent(),
        session.getUser().getUsername(),
        channel.getName(),
        targetSessions.size());
  }
}
