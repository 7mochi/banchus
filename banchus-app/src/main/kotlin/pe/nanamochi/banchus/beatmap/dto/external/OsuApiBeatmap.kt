package pe.nanamochi.banchus.beatmap.dto.external

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@JsonIgnoreProperties(ignoreUnknown = true)
data class OsuApiBeatmap(
    val approved: Int? = null,
    @JsonProperty("submit_date") val submitDate: Instant? = null,
    @JsonProperty("approved_date") val approvedDate: Instant? = null,
    @JsonProperty("last_update") val lastUpdate: Instant? = null,
    val artist: String? = null,
    @JsonProperty("beatmap_id") val beatmapId: Int? = null,
    @JsonProperty("beatmapset_id") val beatmapsetId: Int? = null,
    val bpm: Double = 0.0,
    val creator: String? = null,
    @JsonProperty("creator_id") val creatorId: Int? = null,
    @JsonProperty("difficultyrating") val difficultyRating: Double? = null,
    @JsonProperty("diff_aim") val diffAim: Double? = null,
    @JsonProperty("diff_speed") val diffSpeed: Double? = null,
    @JsonProperty("diff_size") val diffSize: Double? = null,
    @JsonProperty("diff_overall") val diffOverall: Double? = null,
    @JsonProperty("diff_approach") val diffApproach: Double? = null,
    @JsonProperty("diff_drain") val diffDrain: Double? = null,
    @JsonProperty("hit_length") val hitLength: Int? = null,
    val source: String? = null,
    @JsonProperty("genre_id") val genreId: Int? = null,
    @JsonProperty("language_id") val languageId: Int? = null,
    val title: String? = null,
    @JsonProperty("total_length") val totalLength: Int? = null,
    val version: String? = null,
    @JsonProperty("file_md5") val fileMd5: String? = null,
    val mode: Int? = null,
    val tags: String? = null,
    @JsonProperty("favourite_count") val favouriteCount: Int? = null,
    val rating: Double? = null,
    val playcount: Int? = null,
    val passcount: Int? = null,
    @JsonProperty("count_normal") val countNormal: Int? = null,
    @JsonProperty("count_slider") val countSlider: Int? = null,
    @JsonProperty("count_spinner") val countSpinner: Int? = null,
    @JsonProperty("max_combo") val maxCombo: Int? = null,
    val storyboard: Boolean = false,
    val video: Boolean = false,
    @JsonProperty("download_unavailable") val downloadUnavailable: Boolean = false,
    @JsonProperty("audio_unavailable") val audioUnavailable: Boolean = false,
) {
    companion object {
        @JvmStatic
        @JsonCreator
        fun create(props: Map<String, Any?>): OsuApiBeatmap {
            return OsuApiBeatmap(
                approved = props["approved"]?.toString()?.toIntOrNull(),
                submitDate = parseDate(props["submit_date"]),
                approvedDate = parseDate(props["approved_date"]),
                lastUpdate = parseDate(props["last_update"]),
                artist = props["artist"] as String?,
                beatmapId = props["beatmap_id"]?.toString()?.toIntOrNull(),
                beatmapsetId = props["beatmapset_id"]?.toString()?.toIntOrNull(),
                bpm = props["bpm"]?.toString()?.toDoubleOrNull() ?: 0.0,
                creator = props["creator"] as String?,
                creatorId = props["creator_id"]?.toString()?.toIntOrNull(),
                difficultyRating = props["difficultyrating"]?.toString()?.toDoubleOrNull(),
                diffAim = props["diff_aim"]?.toString()?.toDoubleOrNull(),
                diffSpeed = props["diff_speed"]?.toString()?.toDoubleOrNull(),
                diffSize = props["diff_size"]?.toString()?.toDoubleOrNull(),
                diffOverall = props["diff_overall"]?.toString()?.toDoubleOrNull(),
                diffApproach = props["diff_approach"]?.toString()?.toDoubleOrNull(),
                diffDrain = props["diff_drain"]?.toString()?.toDoubleOrNull(),
                hitLength = props["hit_length"]?.toString()?.toIntOrNull(),
                source = props["source"] as String?,
                genreId = props["genre_id"]?.toString()?.toIntOrNull(),
                languageId = props["language_id"]?.toString()?.toIntOrNull(),
                title = props["title"] as String?,
                totalLength = props["total_length"]?.toString()?.toIntOrNull(),
                version = props["version"] as String?,
                fileMd5 = props["file_md5"] as String?,
                mode = props["mode"]?.toString()?.toIntOrNull(),
                tags = props["tags"] as String?,
                favouriteCount = props["favourite_count"]?.toString()?.toIntOrNull(),
                rating = props["rating"]?.toString()?.toDoubleOrNull(),
                playcount = props["playcount"]?.toString()?.toIntOrNull(),
                passcount = props["passcount"]?.toString()?.toIntOrNull(),
                countNormal = props["count_normal"]?.toString()?.toIntOrNull(),
                countSlider = props["count_slider"]?.toString()?.toIntOrNull(),
                countSpinner = props["count_spinner"]?.toString()?.toIntOrNull(),
                maxCombo = props["max_combo"]?.toString()?.toIntOrNull(),
                storyboard = props["storyboard"] == "1",
                video = props["video"] == "1",
                downloadUnavailable = props["download_unavailable"] == "1",
                audioUnavailable = props["audio_unavailable"] == "1",
            )
        }

        private fun parseDate(date: Any?): Instant? {
            val dateStr =
                date?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: return null

            return runCatching {
                    LocalDateTime.parse(dateStr, DATE_FORMATTER).toInstant(ZoneOffset.UTC)
                }
                .getOrNull()
        }
    }
}
