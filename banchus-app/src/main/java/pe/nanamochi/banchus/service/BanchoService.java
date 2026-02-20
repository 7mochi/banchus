package pe.nanamochi.banchus.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.core.Packet;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.packets.server.AnnouncePacket;
import pe.nanamochi.banchus.packets.server.RestartPacket;
import pe.nanamochi.banchus.protocol.PacketHandler;
import pe.nanamochi.banchus.protocol.PacketReader;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;

@Slf4j
@Service
@RequiredArgsConstructor
public class BanchoService {
  private final SessionService sessionService;
  private final PacketBundleService packetBundleService;
  private final PacketWriter packetWriter;
  private final PacketReader packetReader;
  private final PacketHandler packetHandler;

  public byte[] handlePackets(String tokenStr, byte[] rawBody) {
    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    try {
      UUID sessionId = UUID.fromString(tokenStr);

      return sessionService
          .findById(sessionId)
          .map(session -> processPackets(session, rawBody, responseStream))
          .orElseGet(() -> buildRestartResponse(responseStream));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid token received: {}", tokenStr);
      return buildRestartResponse(responseStream);
    } catch (Exception e) {
      log.error("Error handling Bancho request", e);
      return new byte[0];
    }
  }

  private byte[] processPackets(
      Session session, byte[] rawBody, ByteArrayOutputStream responseStream) {
    try {
      session.setLastCommunicatedAt(Instant.now());
      sessionService.update(session);

      if (rawBody != null && rawBody.length > 0) {
        List<Packet> incomingPackets = packetReader.readPackets(rawBody);
        for (Packet packet : incomingPackets) {
          packetHandler.handle(packet, session, responseStream);
        }
      }

      List<PacketBundle> bundles = packetBundleService.dequeueAll(session.getId());
      for (PacketBundle bundle : bundles) {
        responseStream.write(bundle.getData());
      }

      return responseStream.toByteArray();
    } catch (IOException e) {
      log.error("IO Error processing packets for session {}", session.getId(), e);
      return new byte[0];
    }
  }

  private byte[] buildRestartResponse(ByteArrayOutputStream stream) {
    try {
      stream.reset();
      packetWriter.writePacket(stream, new RestartPacket(0));
      packetWriter.writePacket(stream, new AnnouncePacket("The server has restarted."));
      return stream.toByteArray();
    } catch (IOException e) {
      return new byte[0];
    }
  }
}
