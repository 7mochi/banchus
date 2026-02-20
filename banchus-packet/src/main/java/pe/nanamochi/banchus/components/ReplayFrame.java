package pe.nanamochi.banchus.components;

import lombok.Data;

@Data
public class ReplayFrame {
  private int buttonState;
  private int taikoByte;
  private float x;
  private float y;
  private int time;
}
