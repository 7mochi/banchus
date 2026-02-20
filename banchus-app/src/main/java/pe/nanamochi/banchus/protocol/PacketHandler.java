package pe.nanamochi.banchus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packet;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;

@Slf4j
@Component
public class PacketHandler {
  private final Map<Packets, AbstractPacketHandler<?>> handlers;

  public PacketHandler(List<AbstractPacketHandler<?>> beans) {
    this.handlers =
        beans.stream()
            .collect(Collectors.toMap(AbstractPacketHandler::getPacketType, Function.identity()));
  }

  public void handle(Packet packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (packet == null) return;

    AbstractPacketHandler<?> handler = handlers.get(packet.getPacketType());
    if (handler == null) return;

    dispatch(handler, packet, session, responseStream);
  }

  private <T extends Packet> void dispatch(
      AbstractPacketHandler<T> handler,
      Packet packet,
      Session session,
      ByteArrayOutputStream responseStream)
      throws IOException {
    log.debug("Handling packet: {}", packet.getPacketType());
    HandleClientPacket annotation = handler.getClass().getAnnotation(HandleClientPacket.class);
    boolean shouldCheck = (annotation != null) && annotation.checkForRestriction();

    if (shouldCheck && session.getUser().isRestricted()) return;

    handler.handle(handler.getPacketClass().cast(packet), session, responseStream);
  }
}
