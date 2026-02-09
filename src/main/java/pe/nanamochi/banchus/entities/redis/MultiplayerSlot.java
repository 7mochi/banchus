package pe.nanamochi.banchus.entities.redis;

import java.util.UUID;
import lombok.Data;
import pe.nanamochi.banchus.entities.commons.SlotTeam;

@Data
public class MultiplayerSlot {
  private int slotId;
  private UUID sessionId;
  private int userId;
  private int status;
  private SlotTeam team;
  private int mods;
  private boolean loaded;
  private boolean skipped;
}
