package pe.nanamochi.banchus.entities.redis;

import java.time.Instant;
import lombok.Data;
import pe.nanamochi.banchus.entities.commons.MatchStatus;
import pe.nanamochi.banchus.entities.commons.MatchTeamType;
import pe.nanamochi.banchus.entities.commons.Mode;
import pe.nanamochi.banchus.entities.commons.ScoringType;

@Data
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
  private Instant createdAt;
  private Instant updatedAt;

  public MultiplayerMatch() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }
}
