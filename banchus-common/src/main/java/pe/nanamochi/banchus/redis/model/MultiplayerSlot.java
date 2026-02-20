package pe.nanamochi.banchus.redis.model;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.nanamochi.banchus.domain.enums.SlotTeam;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
