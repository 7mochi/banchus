package pe.nanamochi.banchus.repositories.redis;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.entities.commons.SlotStatus;
import pe.nanamochi.banchus.entities.redis.MultiplayerSlot;

@Repository
@RequiredArgsConstructor
public class MultiplayerSlotRepository {
  private final RedisTemplate<String, MultiplayerSlot> redisTemplate;

  public MultiplayerSlot create(int matchId, MultiplayerSlot slot) {
    redisTemplate.opsForValue().set(makeKey(matchId, slot.getSlotId()), slot);
    return slot;
  }

  public MultiplayerSlot findById(int matchId, int slotId) {
    return redisTemplate.opsForValue().get(makeKey(matchId, slotId));
  }

  public MultiplayerSlot findBySessionId(int matchId, UUID sessionId) {
    String pattern = makeKey(matchId);

    return redisTemplate.execute(
        (RedisCallback<MultiplayerSlot>)
            connection -> {
              ScanOptions options = ScanOptions.scanOptions().match(pattern).count(16).build();

              try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                  byte[] keyBytes = cursor.next();
                  byte[] valueBytes = connection.stringCommands().get(keyBytes);

                  if (valueBytes == null) continue;

                  MultiplayerSlot slot =
                      (MultiplayerSlot) redisTemplate.getValueSerializer().deserialize(valueBytes);

                  if (slot != null && sessionId.equals(slot.getSessionId())) {
                    return slot;
                  }
                }
              }
              return null;
            });
  }

  public List<MultiplayerSlot> findAllByMatchId(int matchId) {
    String pattern = makeKey(matchId);
    List<MultiplayerSlot> slots = new ArrayList<>();

    redisTemplate.execute(
        (RedisCallback<Void>)
            connection -> {
              ScanOptions options = ScanOptions.scanOptions().match(pattern).count(16).build();

              try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                  byte[] keyBytes = cursor.next();
                  byte[] valueBytes = connection.stringCommands().get(keyBytes);

                  if (valueBytes != null) {
                    MultiplayerSlot slot =
                        (MultiplayerSlot)
                            redisTemplate.getValueSerializer().deserialize(valueBytes);

                    if (slot != null) {
                      slots.add(slot);
                    }
                  }
                }
              }
              return null;
            });

    slots.sort(Comparator.comparingInt(MultiplayerSlot::getSlotId));
    return slots;
  }

  public MultiplayerSlot update(int matchId, MultiplayerSlot slot) {
    redisTemplate.opsForValue().set(makeKey(matchId, slot.getSlotId()), slot);
    return slot;
  }

  public MultiplayerSlot delete(int matchId, int slotId) {
    MultiplayerSlot slot = findById(matchId, slotId);
    return (slot != null && redisTemplate.delete(makeKey(matchId, slotId))) ? slot : null;
  }

  public boolean allLoaded(int matchId) {
    return findAllByMatchId(matchId).stream()
        .filter(slot -> (slot.getStatus() & SlotStatus.PLAYING.getValue()) != 0)
        .allMatch(MultiplayerSlot::isLoaded);
  }

  public boolean allSkipped(int matchId) {
    return findAllByMatchId(matchId).stream()
        .filter(slot -> (slot.getStatus() & SlotStatus.PLAYING.getValue()) != 0)
        .allMatch(MultiplayerSlot::isSkipped);
  }

  public boolean allCompleted(int matchId) {
    return findAllByMatchId(matchId).stream()
        .filter(slot -> (slot.getStatus() & SlotStatus.PLAYING.getValue()) != 0)
        .allMatch(slot -> (slot.getStatus() & SlotStatus.COMPLETE.getValue()) != 0);
  }

  private String makeKey(int matchId) {
    return "server:match_slots:" + matchId + ":*";
  }

  private String makeKey(int matchId, int slotId) {
    return "server:match_slots:" + matchId + ":" + slotId;
  }
}
