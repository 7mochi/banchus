package pe.nanamochi.banchus.repositories.redis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.entities.redis.MultiplayerMatch;

@Repository
@RequiredArgsConstructor
public class MultiplayerMatchRepository {
  private final RedisTemplate<String, MultiplayerMatch> redisTemplate;

  public MultiplayerMatch create(MultiplayerMatch match) {
    redisTemplate.opsForValue().set(makeKey(String.valueOf(match.getMatchId())), match);
    return match;
  }

  public MultiplayerMatch findById(int matchId) {
    return redisTemplate.opsForValue().get(makeKey(String.valueOf(matchId)));
  }

  public List<MultiplayerMatch> findAll() {
    String matchKey = makeKey("*");
    List<MultiplayerMatch> matches = new ArrayList<>();

    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              ScanOptions options = ScanOptions.scanOptions().match(matchKey).build();

              try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                  byte[] keyBytes = cursor.next();
                  byte[] valueBytes = connection.stringCommands().get(keyBytes);

                  if (valueBytes != null) {
                    MultiplayerMatch match =
                        (MultiplayerMatch)
                            redisTemplate.getValueSerializer().deserialize(valueBytes);

                    if (match != null) {
                      matches.add(match);
                    }
                  }
                }
              }
              return null;
            });

    return matches;
  }

  public MultiplayerMatch update(MultiplayerMatch match) {
    match.setUpdatedAt(Instant.now());
    redisTemplate.opsForValue().set(makeKey(String.valueOf(match.getMatchId())), match);
    return match;
  }

  public MultiplayerMatch delete(int matchId) {
    MultiplayerMatch match = findById(matchId);
    return (match != null && redisTemplate.delete(makeKey(String.valueOf(matchId)))) ? match : null;
  }

  private String makeKey(String matchId) {
    return "server:matches:" + matchId;
  }
}
