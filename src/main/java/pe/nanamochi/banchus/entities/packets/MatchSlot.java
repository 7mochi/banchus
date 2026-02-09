package pe.nanamochi.banchus.entities.packets;

import lombok.Data;
import pe.nanamochi.banchus.entities.commons.SlotTeam;

@Data
public class MatchSlot {
  private int userId;
  private int status;
  private SlotTeam team;
  private int mods;
}
