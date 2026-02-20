package pe.nanamochi.banchus.core;

import java.io.IOException;
import java.io.InputStream;
import pe.nanamochi.banchus.io.IDataReader;

public interface ClientPacket extends Packet {
  void read(IDataReader reader, InputStream stream) throws IOException;
}
