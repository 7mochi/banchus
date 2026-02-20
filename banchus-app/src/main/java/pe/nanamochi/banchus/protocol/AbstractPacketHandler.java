package pe.nanamochi.banchus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import pe.nanamochi.banchus.core.Packet;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;

public abstract class AbstractPacketHandler<T extends Packet> {
  private final Packets packetType;
  private final Class<T> packetClass;

  @SuppressWarnings("unchecked")
  public AbstractPacketHandler() {
    HandleClientPacket handleAnno = this.getClass().getAnnotation(HandleClientPacket.class);
    this.packetType = handleAnno.value();

    ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();
    this.packetClass = (Class<T>) superClass.getActualTypeArguments()[0];
  }

  public final Packets getPacketType() {
    return packetType;
  }

  public final Class<T> getPacketClass() {
    return packetClass;
  }

  public abstract void handle(T packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException;
}
