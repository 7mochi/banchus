package pe.nanamochi.banchus.entities.db;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pe.nanamochi.banchus.entities.CountryCode;
import pe.nanamochi.banchus.entities.Mode;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sessions")
public class Session {
  @Id
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @UuidGenerator
  @Column(name = "id", nullable = false, length = 36, updatable = false)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "utc_offset", nullable = false)
  private int utcOffset;

  @Column(name = "gamemode", nullable = false)
  private Mode gamemode;

  @Column(name = "country", length = 2, nullable = false)
  private CountryCode country;

  @Column(name = "latitude", nullable = false)
  private float latitude;

  @Column(name = "longitude", nullable = false)
  private float longitude;

  @Column(name = "display_city_location", nullable = false)
  private boolean displayCityLocation;

  @Column(name = "action", nullable = false)
  private int action;

  @Column(name = "info_text", length = 128, nullable = false)
  private String infoText;

  @Column(name = "beatmap_md5", length = 32, nullable = false)
  private String beatmapMd5;

  @Column(name = "beatmap_id", nullable = false)
  private int beatmapId;

  @Column(name = "mods", nullable = false)
  private int mods;

  @Column(name = "pm_private", nullable = false)
  private boolean pmPrivate;

  @Column(name = "receive_match_updates", nullable = false)
  private boolean receiveMatchUpdates;

  @Column(name = "spectator_host_session_id")
  private UUID spectatorHostSessionId;

  @Column(name = "away_message", length = 64, nullable = false)
  private String awayMessage;

  @Column(name = "multiplayer_match_id")
  private Integer multiplayerMatchId;

  @Column(name = "last_communicated_at", nullable = false)
  private Instant lastCommunicatedAt;

  @Column(name = "last_np_beatmap_id", nullable = false)
  private int lastNpBeatmapId;

  @Column(name = "is_primary_session", nullable = false)
  private boolean isPrimarySession;

  @Column(name = "osu_version", nullable = false)
  private String osuVersion;

  @Column(name = "osu_path_md5", nullable = false)
  private String osuPathMd5;

  @Column(name = "adapters_str", nullable = false)
  private String adaptersStr;

  @Column(name = "adapters_md5", nullable = false)
  private String adaptersMd5;

  @Column(name = "uninstall_md5", nullable = false)
  private String uninstallMd5;

  @Column(name = "disk_signature_md5", nullable = false)
  private String diskSignatureMd5;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
