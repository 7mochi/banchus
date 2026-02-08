package pe.nanamochi.banchus.entities.packets;

import java.util.List;
import lombok.Data;
import pe.nanamochi.banchus.entities.ReplayAction;

@Data
public class ReplayFrameBundle {
  private ReplayAction action;
  private List<ReplayFrame> frames;
  private ScoreFrame frame;
  private int extra;
  private int sequence;
}
