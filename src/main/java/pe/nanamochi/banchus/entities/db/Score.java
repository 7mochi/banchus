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
  private String beatmapMd5;
  private int score;
  private double performancePoints;
  private double accuracy;
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
  private int timeElapsed;
  @CreatedDate private Instant createdAt;
  @LastModifiedDate private Instant updatedAt;
}
