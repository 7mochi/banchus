package pe.nanamochi.banchus.redis.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;

@Repository
@RequiredArgsConstructor
public class MultiplayerRepository {
  private final RedisTemplate<String, MultiplayerMatch> redisTemplate;
  private final RedisTemplate<String, String> stringRedisTemplate;

  public Long nextMatchId() {
    return stringRedisTemplate.opsForValue().increment("server:last_match_id");
  }

  public MultiplayerMatch create(MultiplayerMatch match) {
    match.setUpdatedAt(Instant.now());
    redisTemplate.opsForValue().set(makeKey(match.getMatchId()), match);
    return findById(match.getMatchId());
  }

  public MultiplayerMatch update(MultiplayerMatch match) {
    return create(match);
  }

  public MultiplayerMatch findById(int matchId) {
    return redisTemplate.opsForValue().get(makeKey(matchId));
  }

  public List<MultiplayerMatch> findAll() {
    String pattern = "server:matches:*";
    List<MultiplayerMatch> matches = new ArrayList<>();

    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
              try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                  byte[] valueBytes = connection.stringCommands().get(cursor.next());
                  if (valueBytes != null) {
                    MultiplayerMatch match =
                        (MultiplayerMatch)
                            redisTemplate.getValueSerializer().deserialize(valueBytes);
                    if (match != null) matches.add(match);
                  }
                }
              }
              return null;
            });
    return matches;
  }

  public void delete(int matchId) {
    redisTemplate.delete(makeKey(matchId));
  }

  private String makeKey(int matchId) {
    return "server:matches:" + matchId;
  }
}
