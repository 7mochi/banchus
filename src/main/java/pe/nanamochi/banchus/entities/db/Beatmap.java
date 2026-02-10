package pe.nanamochi.banchus.entities.db;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import lombok.ToString;
import pe.nanamochi.banchus.entities.commons.BeatmapRankedStatus;
import pe.nanamochi.banchus.entities.commons.Mode;

@Entity
@Data
@ToString(exclude = "beatmapset")
@Table(
    name = "beatmaps",
    indexes = {
      @Index(name = "beatmaps_md5_idx", columnList = "md5"),
      @Index(name = "beatmaps_id_idx", columnList = "id")
    })
public class Beatmap {
  @Id
  @Column(name = "id", nullable = false)
  private int id;

  @Column(name = "mode", nullable = false)
  private Mode mode;

  @Column(name = "md5", length = 32, nullable = false)
  private String md5;

  @Column(name = "status", nullable = false)
  private BeatmapRankedStatus status;

  @Column(name = "version", length = 128, nullable = false)
  private String version;

  @Column(name = "submission_date", nullable = false)
  private Instant submissionDate;

  @Column(name = "last_updated", nullable = false)
  private Instant lastUpdated;

  @Column(name = "playcount", nullable = false)
  private long playcount;

  @Column(name = "passcount", nullable = false)
  private long passcount;

  @Column(name = "total_length", nullable = false)
  private int totalLength;

  @Column(name = "drain_length", nullable = false)
  private int drainLength;

  @Column(name = "count_normal", nullable = false)
  private int countNormal;

  @Column(name = "count_slider", nullable = false)
  private int countSlider;

  @Column(name = "count_spinner", nullable = false)
  private int countSpinner;

  @Column(name = "max_combo", nullable = false)
  private int maxCombo;

  @Column(name = "bpm", nullable = false)
  private double bpm;

  @Column(name = "cs", nullable = false)
  private double cs;

  @Column(name = "ar", nullable = false)
  private double ar;

  @Column(name = "od", nullable = false)
  private double od;

  @Column(name = "hp", nullable = false)
  private double hp;

  @Column(name = "star_rating", nullable = false)
  private double starRating;

  @ManyToOne
  @JoinColumn(name = "beatmapset_id", nullable = false)
  private Beatmapset beatmapset;

  public String createBeatmapChatEmbed() {
    return String.format(
        "[https://osu.ppy.sh/beatmapsets/%d#%s/%d %s - %s (%s) [%s]]",
        beatmapset.getId(),
        mode.getAlias(),
        id,
        beatmapset.getArtistUnicode(),
        beatmapset.getTitleUnicode(),
        beatmapset.getCreator(),
        version);
  }
}
