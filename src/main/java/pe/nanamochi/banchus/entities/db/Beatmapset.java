package pe.nanamochi.banchus.entities.db;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import pe.nanamochi.banchus.entities.commons.BeatmapRankedStatus;

@Entity
@Data
@ToString(exclude = "beatmaps")
@Table(
    name = "beatmapsets",
    indexes = {@Index(name = "beatmapsets_id_idx", columnList = "id")})
public class Beatmapset {
  @Id
  @Column(name = "id", nullable = false)
  private int id;

  @Column(name = "title", length = 128)
  private String title;

  @Column(name = "title_unicode", length = 128)
  private String titleUnicode;

  @Column(name = "artist", length = 128)
  private String artist;

  @Column(name = "artist_unicode", length = 128)
  private String artistUnicode;

  @Column(name = "source", length = 128)
  private String source;

  @Column(name = "source_unicode", length = 128)
  private String sourceUnicode;

  @Column(name = "creator", length = 128)
  private String creator;

  @Column(name = "tags", length = 1024)
  private String tags;

  @Column(name = "submission_status", nullable = false)
  private BeatmapRankedStatus submissionStatus;

  @Column(name = "has_video", nullable = false)
  private boolean hasVideo;

  @Column(name = "has_storyboard", nullable = false)
  private boolean hasStoryboard;

  @Column(name = "submission_date", nullable = false)
  private Instant submissionDate;

  @Column(name = "approved_date")
  private Instant approvedDate;

  @Column(name = "last_updated", nullable = false)
  private Instant lastUpdated;

  @Column(name = "total_playcount", nullable = false)
  private Long totalPlaycount;

  @Column(name = "language_id", nullable = false)
  private int languageId; // TODO: enum?

  @Column(name = "genre_id", nullable = false)
  private int genreId; // TODO: enum?

  @OneToMany(mappedBy = "beatmapset", cascade = CascadeType.ALL)
  private List<Beatmap> beatmaps;
}
