package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UuidGenerator
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.Mode

@Entity
@DynamicUpdate
@Table(name = "sessions")
@EntityListeners(AuditingEntityListener::class)
class Session(
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @UuidGenerator
    @Column(name = "id", nullable = false, length = 36, updatable = false)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,
    @Column(name = "utc_offset", nullable = false) var utcOffset: Int = 0,
    @Column(name = "gamemode", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    var gamemode: Mode = Mode.OSU,
    @Column(name = "country", length = 2, nullable = false)
    @Enumerated(EnumType.STRING)
    var country: CountryCode = CountryCode.XX,
    @Column(name = "latitude", nullable = false) var latitude: Float = 0f,
    @Column(name = "longitude", nullable = false) var longitude: Float = 0f,
    @Column(name = "display_city_location", nullable = false)
    var displayCityLocation: Boolean = false,
    @Column(name = "action", nullable = false) var action: Int = 0,
    @Column(name = "info_text", length = 128, nullable = false) var infoText: String = "",
    @Column(name = "beatmap_md5", length = 32, nullable = false) var beatmapMd5: String = "",
    @Column(name = "beatmap_id", nullable = false) var beatmapId: Int = 0,
    @Column(name = "mods", nullable = false) var mods: Int = 0,
    @Column(name = "pm_private", nullable = false) var pmPrivate: Boolean = false,
    @Column(name = "receive_match_updates", nullable = false)
    var receiveMatchUpdates: Boolean = false,
    @Column(name = "spectator_host_session_id") var spectatorHostSessionId: UUID? = null,
    @Column(name = "away_message", length = 64, nullable = false) var awayMessage: String = "",
    @Column(name = "multiplayer_match_id") var multiplayerMatchId: Int? = -1,
    @Column(name = "last_communicated_at", nullable = false)
    var lastCommunicatedAt: Instant = Instant.now(),
    @Column(name = "last_np_beatmap_id", nullable = false) var lastNpBeatmapId: Int = -1,
    @Column(name = "primary_session", nullable = false) var primarySession: Boolean = false,
    @Column(name = "osu_version", nullable = false) var osuVersion: String = "",
    @Column(name = "osu_path_md5", nullable = false) var osuPathMd5: String = "",
    @Column(name = "adapters_str", nullable = false) var adaptersStr: String = "",
    @Column(name = "adapters_md5", nullable = false) var adaptersMd5: String = "",
    @Column(name = "uninstall_md5", nullable = false) var uninstallMd5: String = "",
    @Column(name = "disk_signature_md5", nullable = false) var diskSignatureMd5: String = "",
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
