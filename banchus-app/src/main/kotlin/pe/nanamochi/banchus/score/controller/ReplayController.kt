package pe.nanamochi.banchus.score.controller

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.auth.service.SessionService
import pe.nanamochi.banchus.core.service.StorageService
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.infrastructure.security.AuthenticatedUser
import pe.nanamochi.banchus.score.service.ScoreService

@RestController
@RequestMapping("/web")
class ReplayController(
    private val sessionService: SessionService,
    private val scoreService: ScoreService,
    private val storageService: StorageService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping(
        value = ["/osu-getreplay.php"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
    )
    fun getReplay(
        @AuthenticatedUser user: User,
        @RequestParam(value = "c") scoreId: Long,
    ): ResponseEntity<ByteArray> {
        val result = binding {
            val sessions = sessionService.fetchByUsername(user.username)
            if (sessions.isEmpty()) {
                HttpStatus.UNAUTHORIZED
            }

            scoreService.fetchOneById(scoreId).toResultOr { HttpStatus.NOT_FOUND }.bind()
            val replayData =
                storageService.getReplay(scoreId).mapError { HttpStatus.NOT_FOUND }.bind()

            log.debug("Serving replay ID: {} to user: {}", scoreId, user.username)

            // TODO: increment replay views for this score, there are things to
            // consider like:
            // - dont increase views fore the player watching their own replay
            // - manage a cooldown so people cant just spam refresh to increase views
            // (use redis for this)

            replayData
        }

        return result.mapBoth(
            success = { bytes ->
                ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes)
            },
            failure = { status -> ResponseEntity.status(status).build() },
        )
    }
}
