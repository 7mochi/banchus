package pe.nanamochi.banchus.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pe.nanamochi.banchus.domain.enums.Mode;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "stats")
public class Stat implements Cloneable {
  @EqualsAndHashCode.Include
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private int id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "gamemode")
  private Mode gamemode;

  @Column(name = "total_score", nullable = false)
  private long totalScore;

  @Column(name = "ranked_score", nullable = false)
  private long rankedScore;

  @Column(name = "performance_points", nullable = false)
  private int performancePoints;

  @Column(name = "play_count", nullable = false)
  private int playCount;

  @Column(name = "play_time", nullable = false)
  private int playTime;

  @Column(name = "accuracy", nullable = false)
  private double accuracy;

  @Column(name = "highest_combo", nullable = false)
  private int highestCombo;

  @Column(name = "total_hits", nullable = false)
  private int totalHits;

  @Column(name = "replay_views", nullable = false)
  private int replayViews;

  @Column(name = "xh_count", nullable = false)
  private int xhCount;

  @Column(name = "x_count", nullable = false)
  private int xCount;

  @Column(name = "sh_count", nullable = false)
  private int shCount;

  @Column(name = "s_count", nullable = false)
  private int sCount;

  @Column(name = "a_count", nullable = false)
  private int aCount;

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
}
