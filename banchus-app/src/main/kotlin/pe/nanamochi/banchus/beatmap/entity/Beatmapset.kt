package pe.nanamochi.banchus.beatmap.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import pe.nanamochi.banchus.beatmap.enums.BeatmapRankedStatus

@Entity
@Table(name = "beatmapsets", indexes = [Index(name = "beatmapsets_id_idx", columnList = "id")])
class Beatmapset(
    @Id @Column(name = "id", nullable = false) var id: Int = 0,
    @Column(name = "title", length = 128) var title: String? = null,
    @Column(name = "title_unicode", length = 128) var titleUnicode: String? = null,
    @Column(name = "artist", length = 128) var artist: String? = null,
    @Column(name = "artist_unicode", length = 128) var artistUnicode: String? = null,
    @Column(name = "source", length = 128) var source: String? = null,
    @Column(name = "source_unicode", length = 128) var sourceUnicode: String? = null,
    @Column(name = "creator", length = 128) var creator: String? = null,
    @Column(name = "tags", length = 1024) var tags: String? = null,
    @Column(name = "submission_status", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    var submissionStatus: BeatmapRankedStatus = BeatmapRankedStatus.PENDING,
    @Column(name = "has_video", nullable = false) var hasVideo: Boolean = false,
    @Column(name = "has_storyboard", nullable = false) var hasStoryboard: Boolean = false,
    @Column(name = "submission_date", nullable = false) var submissionDate: Instant = Instant.now(),
    @Column(name = "approved_date") var approvedDate: Instant? = null,
    @Column(name = "last_updated", nullable = false) var lastUpdated: Instant = Instant.now(),
    @Column(name = "total_playcount", nullable = false) var totalPlaycount: Long = 0L,
    @Column(name = "language_id", nullable = false) var languageId: Int = 0, // TODO: enum?
    @Column(name = "genre_id", nullable = false) var genreId: Int = 0, // TODO: enum?
    @OneToMany(mappedBy = "beatmapset", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var beatmaps: MutableList<Beatmap> = mutableListOf(),
)
