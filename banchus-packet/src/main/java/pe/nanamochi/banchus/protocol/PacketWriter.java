package pe.nanamochi.banchus.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import pe.nanamochi.banchus.core.ServerPacket;
import pe.nanamochi.banchus.io.BanchoDataWriter;
import pe.nanamochi.banchus.io.IDataWriter;

public class PacketWriter {
  private final IDataWriter writer = new BanchoDataWriter();

  public void writePacket(OutputStream stream, ServerPacket packet) throws IOException {
    writer.writeUint16(stream, packet.getPacketType().getId());
    writer.writeUint8(stream, 0);

    ByteArrayOutputStream body = new ByteArrayOutputStream();
    packet.write(this.writer, body);
    byte[] bodyData = body.toByteArray();

    writer.writeInt32(stream, bodyData.length);
    stream.write(bodyData);
  }
}
