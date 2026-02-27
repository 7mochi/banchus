package pe.nanamochi.banchus.controller.web

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.LeaderboardType
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.domain.enums.SubmissionStatus
import pe.nanamochi.banchus.infrastructure.security.AuthenticatedUser
import pe.nanamochi.banchus.service.BeatmapService
import pe.nanamochi.banchus.service.ScoreService

@RestController
@RequestMapping("/web")
class LeaderboardController(
    private val beatmapService: BeatmapService,
    private val scoreService: ScoreService,
) {
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
                val beatmap = beatmapService.getOrCreateBeatmap(beatmapMd5).bind()
                val type = LeaderboardType.fromValue(leaderboardType)
                val mode = Mode.fromValue(gamemode)

                val modsToFilter = modsBitmask.takeIf { type == LeaderboardType.MODS }
                val country = user.country.takeIf { type == LeaderboardType.COUNTRY }

                val scores =
                    scoreService.fetchLeaderboard(
                        beatmap,
                        mode,
                        modsToFilter,
                        SubmissionStatus.BEST,
                        country,
                    )
                val personalBest = scoreService.fetchBest(beatmap, user).get()

                scoreService.formatLeaderboardResponse(scores, personalBest, user, beatmap)
            }
            .mapBoth(
                success = { ResponseEntity.ok(it) },
                failure = {
                    log.error("Error fetching leaderboard for beatmap {}: {}", beatmapMd5, it)
                    ResponseEntity.ok("-1|false")
                },
            )
}
