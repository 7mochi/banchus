package pe.nanamochi.banchus.controller.client.web

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.BeatmapDirectDisplayMode
import pe.nanamochi.banchus.infrastructure.client.OsuDirectApiClient
import pe.nanamochi.banchus.infrastructure.security.AuthenticatedUser

@RestController
@RequestMapping("/web")
class DirectController(private val osuDirectApiService: OsuDirectApiClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/osu-search.php", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun osuSearchHandler(
        @AuthenticatedUser user: User,
        @RequestParam("r") displayMode: BeatmapDirectDisplayMode,
        @RequestParam("p") pageOffset: Int,
        @RequestParam("q") query: String,
        @RequestParam("m") mode: Int,
    ): ResponseEntity<String> {
        return osuDirectApiService.search(query, mode, displayMode, pageOffset)?.let {
            ResponseEntity.ok(it)
        }
            ?: run {
                log.warn(
                    "osu!direct search failed for query: query='{}', mode='{}', displayMode='{}', pageOffset={}",
                    query,
                    mode,
                    displayMode,
                    pageOffset,
                )
                ResponseEntity.ok("-1\nFailed to retrieve data from the beatmap mirror.")
            }
    }
}
