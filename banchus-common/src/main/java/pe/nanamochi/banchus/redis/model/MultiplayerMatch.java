package pe.nanamochi.banchus.redis.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import pe.nanamochi.banchus.domain.enums.*;

@Data
@Builder
@AllArgsConstructor
public class MultiplayerMatch {
  private int matchId;
  private String matchName;
  private String matchPassword;
  private String beatmapName;
  private int beatmapId;
  private String beatmapMd5;
  private int hostUserId;
  private Mode mode;
  private int mods;
  private ScoringType scoringType;
  private MatchTeamType teamType;
  private boolean freemodsEnabled;
  private int randomSeed;
  private MatchStatus status;
  private List<MultiplayerSlot> slots = new ArrayList<>();
  private Instant createdAt;
  private Instant updatedAt;

  public MultiplayerMatch() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public boolean allLoaded() {
    return slots.stream()
        .filter(slot -> (slot.getStatus() & SlotStatus.PLAYING.getValue()) != 0)
        .allMatch(MultiplayerSlot::isLoaded);
  }

  public boolean allSkipped() {
    return slots.stream()
        .filter(slot -> (slot.getStatus() & SlotStatus.PLAYING.getValue()) != 0)
        .allMatch(MultiplayerSlot::isSkipped);
  }

  public boolean allCompleted() {
    return slots.stream()
        .filter(slot -> (slot.getStatus() & SlotStatus.PLAYING.getValue()) != 0)
        .allMatch(slot -> (slot.getStatus() & SlotStatus.COMPLETE.getValue()) != 0);
  }
}
