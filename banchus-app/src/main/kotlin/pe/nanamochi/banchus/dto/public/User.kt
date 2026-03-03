package pe.nanamochi.banchus.dto.public

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.OffsetDateTime

data class User(
    val id: Int,
    val username: String,
    @JsonProperty("profile_colour") val profileColour: String?,
    @JsonProperty("avatar_url") val avatarUrl: String,
    @JsonProperty("country_code") val countryCode: String,
    @JsonProperty("is_active") val isActive: Boolean,
    @JsonProperty("is_bot") val isBot: Boolean,
    @JsonProperty("is_deleted") val isDeleted: Boolean,
    @JsonProperty("is_online") val isOnline: Boolean,
    @JsonProperty("is_supporter") val isSupporter: Boolean,
    @JsonProperty("last_visit") val lastVisit: OffsetDateTime?,
    @JsonProperty("pm_friends_only") val pmFriendsOnly: Boolean,
    @JsonProperty("default_group") val defaultGroup: String? = null,
)

data class UserExtended(
    @get:JsonUnwrapped val base: User,
    @JsonProperty("cover_url") val coverUrl: String,
    val discord: String?,
    @JsonProperty("has_supported") val hasSupported: Boolean,
    val interests: String?,
    @JsonProperty("join_date") val joinDate: OffsetDateTime,
    val location: String?,
    @JsonProperty("max_blocks") val maxBlocks: Int,
    @JsonProperty("max_friends") val maxFriends: Int,
    val occupation: String?,
    val playmode: String,
    val playstyle: List<String>,
    @JsonProperty("post_count") val postCount: Int,
    @JsonProperty("profile_hue") val profileHue: Int?,
    @JsonProperty("profile_order") val profileOrder: List<String>,
    val title: String?,
    val twitter: String?,
    val website: String?,
    val kudosu: Kudosu,
    val country: UserCountry,
    val cover: UserCover,
    @JsonProperty("is_restricted") val isRestricted: Boolean,
    val statistics: UserStatistics,
    @JsonProperty("support_level") val supportLevel: Int,
    val badges: List<UserBadge> = emptyList(),
    @JsonProperty("user_achievements") val achievements: List<UserAchievement> = emptyList(),
    @JsonProperty("rank_history") val rankHistory: RankHistory? = null,
)

data class Kudosu(val total: Int, val available: Int)

data class UserCountry(val code: String, val name: String)

data class UserCover(
    @JsonProperty("custom_url") val customUrl: String?,
    val url: String?,
    val id: Int?,
)

data class UserBadge(
    @JsonProperty("awarded_at") val awardedAt: OffsetDateTime,
    val description: String,
    @JsonProperty("image@2x_url") val image2xUrl: String,
    @JsonProperty("image_url") val imageUrl: String,
    val url: String,
)

data class UserAchievement(
    @JsonProperty("achieved_at") val achievedAt: OffsetDateTime,
    @JsonProperty("achievement_id") val achievementId: Int,
)

data class RankHistory(val mode: String, val data: List<Int>)

data class UserStatistics(
    val level: UserLevel,
    val pp: Double,
    @JsonProperty("global_rank") val globalRank: Int?,
    @JsonProperty("ranked_score") val rankedScore: Long,
    @JsonProperty("hit_accuracy") val hitAccuracy: Double,
    val accuracy: Double,
    @JsonProperty("play_count") val playCount: Int,
    @JsonProperty("play_time") val playTime: Int,
    @JsonProperty("total_score") val totalScore: Long,
    @JsonProperty("total_hits") val totalHits: Long,
    @JsonProperty("maximum_combo") val maximumCombo: Int,
    @JsonProperty("replays_watched_by_others") val replaysWatchedByOthers: Int,
    @JsonProperty("is_ranked") val isRanked: Boolean,
    @JsonProperty("grade_counts") val gradeCounts: GradeCounts,
    val rank: UserRank,
)

data class UserLevel(val current: Int, val progress: Int)

data class GradeCounts(val ss: Int, val ssh: Int, val s: Int, val sh: Int, val a: Int)

data class UserRank(val global: Int?, val country: Int?)
