package pe.nanamochi.banchus.entities.db;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import pe.nanamochi.banchus.entities.BeatmapRankedStatus;
import pe.nanamochi.banchus.entities.Mode;

@Entity
@Data
@Table(name = "beatmaps")
public class Beatmap {
  @Id private int id;

  private Mode mode;
  private String md5;
  private BeatmapRankedStatus status;
  private String version;
  private Instant submissionDate;
  private Instant lastUpdated;
  private int playcount;
  private int passcount;
  private int totalLength;
  private int drainLength;
  private int countNormal;
  private int countSlider;
  private int countSpinner;
  private int maxCombo;
  private double bpm;
  private double cs;
  private double ar;
  private double od;
  private double hp;
  private double starRating;

  @ManyToOne
  @JoinColumn(name = "beatmapset_id")
  private Beatmapset beatmapset;
}
