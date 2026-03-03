package pe.nanamochi.banchus.controller.public

import com.github.michaelbull.result.*
import java.time.OffsetDateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.dto.public.GradeCounts
import pe.nanamochi.banchus.dto.public.Kudosu
import pe.nanamochi.banchus.dto.public.UserCountry
import pe.nanamochi.banchus.dto.public.UserCover
import pe.nanamochi.banchus.dto.public.UserExtended
import pe.nanamochi.banchus.dto.public.UserLevel
import pe.nanamochi.banchus.dto.public.UserRank
import pe.nanamochi.banchus.dto.public.UserStatistics
import pe.nanamochi.banchus.service.UserService

@RestController
@RequestMapping("/api/v1/users")
class UserApiController(
    private val userService: UserService,
    @Value($$"${banchus.domain-url}") private val domainUrl: String,
) {

    @GetMapping(value = ["/{user}/{mode}", "/{user}"])
    fun getUser(
        @PathVariable user: String,
        @PathVariable(required = false) mode: String?,
        @RequestParam(required = false) key: String?,
    ): ResponseEntity<*> =
        binding { resolveUser(user, key).bind() }
            .mapBoth(
                success = { player -> ResponseEntity.ok(player.toExtendedUserDto()) },
                failure = { ResponseEntity.status(404).body(mapOf("error" to "User not found")) },
            )

    private fun resolveUser(identifier: String, key: String?): Result<User, UserNotFound> =
        binding {
            when {
                identifier.startsWith("@") ->
                    userService.findByUsername(identifier.removePrefix("@")).bind()
                key == "username" -> userService.findByUsername(identifier).bind()
                key == "id" ->
                    identifier.toIntOrResult().bind().let { userService.findById(it).bind() }
                else -> {
                    identifier
                        .toIntOrResult()
                        .flatMap { id -> userService.findById(id) }
                        .orElse { userService.findByUsername(identifier) }
                        .bind()
                }
            }
        }

    private fun String.toIntOrResult(): Result<Int, UserNotFound> =
        this.toIntOrNull().toResultOr { UserNotFound }

    fun User.toExtendedUserDto(): UserExtended {
        return UserExtended(
            base =
                pe.nanamochi.banchus.dto.public.User(
                    id = this.id,
                    username = this.username,
                    profileColour = "#000000",
                    avatarUrl = "https://${domainUrl}/${this.id}",
                    countryCode = this.country.code,
                    isActive = true,
                    isBot = false,
                    isDeleted = false,
                    isOnline = false,
                    isSupporter = true,
                    lastVisit = OffsetDateTime.now(),
                    pmFriendsOnly = false,
                ),
            coverUrl = "https://assets.ppy.sh/user-profile-covers/1/default.jpeg",
            discord = null,
            hasSupported = true,
            interests = null,
            joinDate = OffsetDateTime.now(),
            location = null,
            maxBlocks = 50,
            maxFriends = 500,
            occupation = null,
            playmode = "osu",
            playstyle = listOf("mouse"),
            postCount = 0,
            profileHue = 0,
            profileOrder = listOf("me", "recent_activity", "beatmaps"),
            title = null,
            twitter = null,
            website = null,
            kudosu = Kudosu(0, 0),
            country = UserCountry(this.country.code, "Unknown"),
            cover = UserCover(null, null, null),
            isRestricted = false,
            supportLevel = 3,
            statistics =
                UserStatistics(
                    level = UserLevel(1, 0),
                    pp = 0.0,
                    globalRank = null,
                    rankedScore = 0,
                    hitAccuracy = 0.0,
                    accuracy = 0.0,
                    playCount = 0,
                    playTime = 0,
                    totalScore = 0,
                    totalHits = 0,
                    maximumCombo = 0,
                    replaysWatchedByOthers = 0,
                    isRanked = true,
                    gradeCounts = GradeCounts(0, 0, 0, 0, 0),
                    rank = UserRank(null, null),
                ),
        )
    }
}
