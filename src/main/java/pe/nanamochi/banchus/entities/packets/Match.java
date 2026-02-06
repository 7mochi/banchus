package pe.nanamochi.banchus.entities.packets;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.entities.MatchTeamType;
import pe.nanamochi.banchus.entities.MatchType;
import pe.nanamochi.banchus.entities.Mode;
import pe.nanamochi.banchus.entities.ScoringType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
  private int id;
  private boolean inProgress;
  private MatchType type;
  private int mods;
  private String name;
  private String password;
  private String beatmapName;
  private int beatmapId;
  private String beatmapMd5;
  private List<MatchSlot> slots;
  private int hostId;
  private Mode mode;
  private ScoringType scoringType;
  private MatchTeamType teamType;
  private boolean freemodsEnabled;
  private int randomSeed;
}
