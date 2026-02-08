package pe.nanamochi.banchus.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MultiplayerMatchIdRepository {
  private final RedisTemplate<String, String> redisTemplate;

  public Long incrementId() {
    ValueOperations<String, String> operations = redisTemplate.opsForValue();
    String key = makeKey();
    return operations.increment(key);
  }

  public void setId(int id) {
    ValueOperations<String, String> operations = redisTemplate.opsForValue();
    String key = makeKey();
    operations.set(key, String.valueOf(id));
  }

  private String makeKey() {
    return "server:last_match_id";
  }
}
