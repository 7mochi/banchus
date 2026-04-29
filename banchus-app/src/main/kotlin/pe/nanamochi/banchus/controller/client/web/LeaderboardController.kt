package pe.nanamochi.banchus.controller.client.web

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.infrastructure.security.AuthenticatedUser
import pe.nanamochi.banchus.service.LeaderboardService

@RestController
@RequestMapping("/web")
class LeaderboardController(private val leaderboardService: LeaderboardService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/osu-osz2-getscores.php", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getLeaderboard(
        @AuthenticatedUser user: User,
        @RequestParam(name = "v") leaderboardType: Int,
        @RequestParam(name = "c") beatmapMd5: String,
        @RequestParam(name = "m") gamemode: Int,
        @RequestParam(name = "mods") modsBitmask: Int,
        @RequestParam(name = "f") filename: String,
        @RequestParam(name = "i") beatmapSetId: Int,
        @RequestParam(name = "vv") version: Int,
        @RequestParam(name = "s") skip: String,
        @RequestParam(name = "h") hash: String,
        @RequestParam(name = "a") aqn: String,
    ): ResponseEntity<String> =
        binding {
                leaderboardService
                    .fetchBeatmapLeaderboard(
                        user,
                        beatmapMd5,
                        leaderboardType,
                        gamemode,
                        modsBitmask,
                    )
                    .bind()
            }
            .mapBoth(
                success = { ResponseEntity.ok(it) },
                failure = {
                    log.error("Error fetching leaderboard for beatmap $beatmapMd5: $it")
                    ResponseEntity.ok("-1|false")
                },
            )
}
