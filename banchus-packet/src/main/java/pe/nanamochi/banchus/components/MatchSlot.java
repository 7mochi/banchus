package pe.nanamochi.banchus.components;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSlot {
  private int userId;
  private int status;
  private SlotTeam team;
  private int mods;
}
