package pe.nanamochi.banchus.controller.resource

import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.infrastructure.security.AuthenticatedUser
import pe.nanamochi.banchus.service.UserService

@RestController
@RequestMapping("/d")
class BeatmapSetController(private val userService: UserService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{beatmapSetId}")
    fun downloadBeatmapSet(
        @PathVariable beatmapSetId: String,
        @AuthenticatedUser user: User,
        @RequestParam("vv") endpointVersion: Int,
    ): ResponseEntity<String> {
        log.debug("Beatmap download request: user=${user.username}, setId=$beatmapSetId")

        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
            .location(URI.create("https://osu.direct/d/$beatmapSetId"))
            .body("")
    }
}
