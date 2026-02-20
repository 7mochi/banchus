package pe.nanamochi.banchus.redis.model;

import java.time.Instant;
import lombok.Data;

@Data
public class PacketBundle {
  private byte[] data;
  private Instant createdAt;

  public PacketBundle(byte[] data) {
    this.data = data;
    this.createdAt = Instant.now();
  }
}
