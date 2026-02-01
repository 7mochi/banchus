package pe.nanamochi.banchus.entities.db;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pe.nanamochi.banchus.entities.Mode;
import pe.nanamochi.banchus.entities.SubmissionStatus;

@Entity
@Data
@Table(name = "scores")
@EntityListeners(AuditingEntityListener.class)
public class Score {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @ManyToOne private User user;
  private String onlineChecksum;

  @ManyToOne private Beatmap beatmap;

  private int score;
  private double performancePoints;
  private float accuracy;
  private int highestCombo;
  private boolean fullCombo;
  private int mods;
  private int num300s;
  private int num100s;
  private int num50s;
  private int numMisses;
  private int numGekis;
  private int numKatus;
  private String grade;
  private SubmissionStatus submissionStatus;
  private Mode mode;
  private boolean passed;
  private int timeElapsed;
  @CreatedDate private Instant createdAt;
  @LastModifiedDate private Instant updatedAt;

  @PrePersist
  @PreUpdate
  private void updateAccuracy() {
    this.accuracy = calculateAccuracy();
  }

  public float calculateAccuracy() {
    if (mode == null) return 0f;
    return switch (mode) {
      case OSU -> calculateOsuAccuracy();
      case TAIKO -> calculateTaikoAccuracy();
      case CATCH -> calculateCatchAccuracy();
      case MANIA -> calculateManiaAccuracy();
    };
  }

  private float calculateOsuAccuracy() {
    int totalNotes = this.getNum300s() + this.getNum100s() + this.getNum50s() + this.getNumMisses();
    float accuracy =
        (100.0f
            * ((this.getNum300s() * 300.0f)
                + (this.getNum100s() * 100.0f)
                + (this.getNum50s() * 50.0f))
            / (totalNotes * 300.0f));
    return clampAccuracy(accuracy);
  }

  private float calculateTaikoAccuracy() {
    int totalNotes = this.getNum300s() + this.getNum100s() + this.getNumMisses();
    float accuracy = (100.0f * ((this.getNum100s() * 0.5f) + this.getNum300s()) / totalNotes);
    return clampAccuracy(accuracy);
  }

  private float calculateCatchAccuracy() {
    int totalNotes =
        this.getNum300s()
            + this.getNum100s()
            + this.getNum50s()
            + this.getNumKatus()
            + this.getNumMisses();
    float accuracy =
        (100.0f * (this.getNum300s() + this.getNum100s() + this.getNum50s())) / totalNotes;
    return clampAccuracy(accuracy);
  }

  private float calculateManiaAccuracy() {
    int totalNotes =
        this.getNum300s()
            + this.getNum100s()
            + this.getNum50s()
            + this.getNumGekis()
            + this.getNumKatus()
            + this.getNumMisses();
    float accuracy =
        (100.0f
            * ((this.getNum50s() * 50.0f)
                + (this.getNum100s() * 100.0f)
                + (this.getNumKatus() * 200.0f)
                + ((this.getNum300s() + this.getNumGekis()) * 300.0f))
            / (totalNotes * 300.0f));
    return clampAccuracy(accuracy);
  }

  private float clampAccuracy(float value) {
    return Math.min(100f, Math.max(0f, value));
  }
}
