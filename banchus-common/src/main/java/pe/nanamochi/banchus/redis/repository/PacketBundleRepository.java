package pe.nanamochi.banchus.redis.repository;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.redis.model.PacketBundle;

@Slf4j
@Repository
public class PacketBundleRepository {
  private final RedisTemplate<String, PacketBundle> redisTemplate;
  private static final int warningQueueSizeThreshold = 100;

  public PacketBundleRepository(RedisTemplate<String, PacketBundle> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void enqueue(UUID sessionId, PacketBundle packetBundle) {
    Long queueSize = redisTemplate.opsForList().rightPush(makeKey(sessionId), packetBundle);

    if (queueSize > warningQueueSizeThreshold) {
      log.warn("Packet bundle size exceeded the warning threshold for Session ID: {}", sessionId);
    }
  }

  public PacketBundle dequeueOne(UUID sessionId) {
    return redisTemplate.opsForList().leftPop(makeKey(sessionId));
  }

  public List<PacketBundle> dequeueAll(UUID sessionId) {
    List<PacketBundle> bundles = redisTemplate.opsForList().range(makeKey(sessionId), 0, -1);
    if (!bundles.isEmpty()) {
      redisTemplate.delete(makeKey(sessionId));
    }
    return bundles;
  }

  private String makeKey(UUID sessionId) {
    return "server:packet-bundles:" + sessionId;
  }
}
