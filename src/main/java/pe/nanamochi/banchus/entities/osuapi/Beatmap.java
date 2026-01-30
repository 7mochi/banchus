package pe.nanamochi.banchus.entities.osuapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Beatmap {

  private int approved;

  @JsonProperty("submit_date")
  private Instant submitDate;

  @JsonProperty("approved_date")
  private Instant approvedDate;

  @JsonProperty("last_update")
  private Instant lastUpdate;

  private String artist;

  @JsonProperty("beatmap_id")
  private int beatmapId;

  @JsonProperty("beatmapset_id")
  private int beatmapsetId;

  private double bpm;
  private String creator;

  @JsonProperty("creator_id")
  private int creatorId;

  @JsonProperty("difficultyrating")
  private double difficultyRating;

  @JsonProperty("diff_aim")
  private double diffAim;

  @JsonProperty("diff_speed")
  private double diffSpeed;

  @JsonProperty("diff_size")
  private double diffSize;

  @JsonProperty("diff_overall")
  private double diffOverall;

  @JsonProperty("diff_approach")
  private double diffApproach;

  @JsonProperty("diff_drain")
  private double diffDrain;

  @JsonProperty("hit_length")
  private int hitLength;

  private String source;

  @JsonProperty("genre_id")
  private int genreId;

  @JsonProperty("language_id")
  private int languageId;

  private String title;

  @JsonProperty("total_length")
  private int totalLength;

  private String version;

  @JsonProperty("file_md5")
  private String fileMd5;

  private int mode;
  private String tags;

  @JsonProperty("favourite_count")
  private int favouriteCount;

  private double rating;
  private int playcount;
  private int passcount;

  @JsonProperty("count_normal")
  private int countNormal;

  @JsonProperty("count_slider")
  private int countSlider;

  @JsonProperty("count_spinner")
  private int countSpinner;

  @JsonProperty("max_combo")
  private int maxCombo;

  private boolean storyboard;
  private boolean video;

  @JsonProperty("download_unavailable")
  private boolean downloadUnavailable;

  @JsonProperty("audio_unavailable")
  private boolean audioUnavailable;

  @JsonProperty("submit_date")
  public void setSubmitDate(String submitDate) {
    this.submitDate =
        LocalDateTime.parse(submitDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC);
  }

  @JsonProperty("approved_date")
  public void setApprovedDate(String approvedDate) {
    this.approvedDate =
        LocalDateTime.parse(approvedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC);
  }

  public void setLastUpdate(String lastUpdate) {
    this.lastUpdate =
        LocalDateTime.parse(lastUpdate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC);
  }

  @JsonProperty("storyboard")
  public void setStoryboard(String value) {
    this.storyboard = "1".equals(value);
  }

  @JsonProperty("video")
  public void setVideo(String value) {
    this.video = "1".equals(value);
  }

  @JsonProperty("download_unavailable")
  public void setDownloadUnavailable(String value) {
    this.downloadUnavailable = "1".equals(value);
  }

  @JsonProperty("audio_unavailable")
  public void setAudioUnavailable(String value) {
    this.audioUnavailable = "1".equals(value);
  }
}
