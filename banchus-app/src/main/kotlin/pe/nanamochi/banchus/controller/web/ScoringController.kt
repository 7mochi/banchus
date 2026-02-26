package pe.nanamochi.banchus.controller.web

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pe.nanamochi.banchus.domain.enums.ScoreSubmissionErrors
import pe.nanamochi.banchus.domain.errors.BeatmapNotFound
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.InvalidCredentials
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.service.BeatmapService
import pe.nanamochi.banchus.service.ScoreService
import pe.nanamochi.banchus.service.SessionService
import pe.nanamochi.banchus.service.UserService

@RestController
@RequestMapping("/web")
class ScoringController(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val scoreService: ScoreService,
    private val beatmapService: BeatmapService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping(
        value = ["/osu-submit-modular-selector.php"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun scoreSubmission(
        request: HttpServletRequest,
        @RequestParam(value = "iv") ivB64: String,
        @RequestParam(value = "st") scoreTime: Int,
        @RequestParam(value = "pass") passwordMd5: String,
        @RequestParam(value = "osuver") osuVersion: String,
        @RequestPart(value = "i", required = false) screenshot: MultipartFile?,
    ): String {
        return binding {
                val parsedScore =
                    scoreService.parseScore(request, ivB64, osuVersion, scoreTime).bind()
                val user = userService.login(parsedScore.username, passwordMd5).bind()
                val session = sessionService.findPrimaryByUsername(user.username).bind()
                val beatmap = beatmapService.getOrCreateBeatmap(parsedScore.beatmapMd5).bind()

                scoreService.processScoreSubmission(parsedScore, user, beatmap, session).bind()
            }
            .mapBoth(
                success = { chartString -> chartString },
                failure = { domainError ->
                    val submissionError = mapToSubmissionError(domainError)
                    log.error(
                        "Submission failed: {} -> osu! error: {}",
                        domainError,
                        submissionError.value,
                    )
                    "error: ${submissionError.value}"
                },
            )
    }

    private fun mapToSubmissionError(error: DomainMessage): ScoreSubmissionErrors {
        return when (error) {
            is InvalidCredentials -> ScoreSubmissionErrors.NEEDS_AUTHENTICATION
            is UserNotFound -> ScoreSubmissionErrors.NO_SUCH_USER
            is BeatmapNotFound -> ScoreSubmissionErrors.BEATMAP_UNRANKED
            is InternalError -> ScoreSubmissionErrors.NO
            else -> ScoreSubmissionErrors.NO
        }
    }
}
