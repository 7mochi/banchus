package pe.nanamochi.banchus.entities.db;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pe.nanamochi.banchus.entities.commons.Mode;
import pe.nanamochi.banchus.entities.commons.SubmissionStatus;

@Entity
@Data
@Table(
    name = "scores",
    indexes = {
      @Index(
          name = "score_user_mode_status_pp_idx",
          columnList = "user_id, mode, submission_status, performance_points DESC"),
      @Index(name = "beatmap_mode_status_idx", columnList = "beatmap_id"),
      @Index(name = "beatmap_status_idx", columnList = "submissionStatus")
    })
@EntityListeners(AuditingEntityListener.class)
public class Score {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private int id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "online_checksum", length = 32, nullable = false)
  private String onlineChecksum;

  @ManyToOne
  @JoinColumn(name = "beatmap_id", nullable = false)
  private Beatmap beatmap;

  @Column(name = "score", nullable = false)
  private long score;

  @Column(name = "performance_points", nullable = false)
  private double performancePoints;

  @Column(name = "accuracy", nullable = false)
  private double accuracy;

  @Column(name = "highest_combo", nullable = false)
  private int highestCombo;

  @Column(name = "full_combo", nullable = false)
  private boolean fullCombo;

  @Column(name = "mods", nullable = false)
  private int mods;

  @Column(name = "num_300s", nullable = false)
  private int num300s;

  @Column(name = "num_100s", nullable = false)
  private int num100s;

  @Column(name = "num_50s", nullable = false)
  private int num50s;

  @Column(name = "num_misses", nullable = false)
  private int numMisses;

  @Column(name = "num_gekis", nullable = false)
  private int numGekis;

  @Column(name = "num_katus", nullable = false)
  private int numKatus;

  @Column(name = "grade", length = 2, nullable = false)
  private String grade;

  @Column(name = "submission_status", nullable = false)
  private SubmissionStatus submissionStatus;

  @Column(name = "mode", nullable = false)
  private Mode mode;

  @Column(name = "passed", nullable = false)
  private boolean passed;

  @Column(name = "time_elapsed", nullable = false)
  private int timeElapsed;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  public void updateAccuracy() {
    this.accuracy = calculateAccuracy();
  }

  public double calculateAccuracy() {
    if (mode == null) return 0.0;
    return switch (mode) {
      case OSU -> calculateOsuAccuracy();
      case TAIKO -> calculateTaikoAccuracy();
      case CATCH -> calculateCatchAccuracy();
      case MANIA -> calculateManiaAccuracy();
    };
  }

  private double calculateOsuAccuracy() {
    int totalNotes = this.getNum300s() + this.getNum100s() + this.getNum50s() + this.getNumMisses();
    if (totalNotes == 0) return 0.0;

    double accuracy =
        (100.0
            * ((this.getNum300s() * 300.0)
                + (this.getNum100s() * 100.0)
                + (this.getNum50s() * 50.0))
            / (totalNotes * 300.0));
    return clampAccuracy(accuracy);
  }

  private double calculateTaikoAccuracy() {
    int totalNotes = this.getNum300s() + this.getNum100s() + this.getNumMisses();
    if (totalNotes == 0) return 0.0;

    double accuracy = (100.0 * ((this.getNum100s() * 0.5) + this.getNum300s()) / totalNotes);
    return clampAccuracy(accuracy);
  }

  private double calculateCatchAccuracy() {
    int totalNotes =
        this.getNum300s()
            + this.getNum100s()
            + this.getNum50s()
            + this.getNumKatus()
            + this.getNumMisses();
    if (totalNotes == 0) return 0.0;

    double accuracy =
        (100.0 * (this.getNum300s() + this.getNum100s() + this.getNum50s())) / totalNotes;
    return clampAccuracy(accuracy);
  }

  private double calculateManiaAccuracy() {
    int totalNotes =
        this.getNum300s()
            + this.getNum100s()
            + this.getNum50s()
            + this.getNumGekis()
            + this.getNumKatus()
            + this.getNumMisses();
    if (totalNotes == 0) return 0.0;

    double accuracy =
        (100.0
            * ((this.getNum50s() * 50.0)
                + (this.getNum100s() * 100.0)
                + (this.getNumKatus() * 200.0)
                + ((this.getNum300s() + this.getNumGekis()) * 300.0))
            / (totalNotes * 300.0));
    return clampAccuracy(accuracy);
  }

  private double clampAccuracy(double value) {
    return Math.min(100.0, Math.max(0, value));
  }
}
