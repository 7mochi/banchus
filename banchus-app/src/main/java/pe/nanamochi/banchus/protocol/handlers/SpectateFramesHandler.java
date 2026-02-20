package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.client.SpectateFramesPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.PacketBundleService;
import pe.nanamochi.banchus.service.SpectatorService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(value = Packets.OSU_SPECTATE_FRAMES, checkForRestriction = true)
public class SpectateFramesHandler extends AbstractPacketHandler<SpectateFramesPacket> {
  private final PacketWriter packetWriter;
  private final PacketBundleService packetBundleService;
  private final SpectatorService spectatorService;

  @Override
  public void handle(
      SpectateFramesPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getUser().isRestricted()) {
      return;
    }

    // Check if there are any spectators before fetching the full list
    if (!spectatorService.hasSpectators(session.getId())) {
      return;
    }

    for (UUID spectateSessionId : spectatorService.getSpectators(session.getId())) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      packetWriter.writePacket(
          stream,
          new pe.nanamochi.banchus.packets.server.SpectateFramesPacket(
              packet.getReplayFrameBundle()));
      packetBundleService.enqueue(spectateSessionId, new PacketBundle(stream.toByteArray()));
    }
  }
}
