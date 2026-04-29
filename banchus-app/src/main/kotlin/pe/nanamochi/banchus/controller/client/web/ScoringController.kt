package pe.nanamochi.banchus.controller.client.web

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pe.nanamochi.banchus.domain.enums.ScoreSubmissionErrors
import pe.nanamochi.banchus.domain.error.InternalError
import pe.nanamochi.banchus.service.ScoreService

@RestController
@RequestMapping("/web")
class ScoringController(private val scoreService: ScoreService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping(
        value = ["/osu-submit-modular-selector.php"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun scoreSubmission(
        request: HttpServletRequest,
        @RequestHeader headers: HttpHeaders,
        @RequestParam(value = "iv") ivB64: String,
        @RequestParam(value = "s") clientHashB64: String,
        @RequestParam(value = "st") scoreTime: Int,
        @RequestParam(value = "pass") passwordMd5: String,
        @RequestParam(value = "osuver") osuVersion: String,
        @RequestPart(value = "i", required = false) screenshot: MultipartFile?,
    ): String {
        return binding {
                scoreService
                    .submitScore(
                        request,
                        headers,
                        ivB64,
                        clientHashB64,
                        scoreTime,
                        passwordMd5,
                        osuVersion,
                    )
                    .bind()
            }
            .mapBoth(
                success = { chartString -> chartString },
                failure = { domainError ->
                    val submissionError =
                        when (domainError) {
                            // TODO: add more specific errors
                            is InternalError -> ScoreSubmissionErrors.NO
                            else -> ScoreSubmissionErrors.NO
                        }
                    "error: ${submissionError.value}"
                },
            )
    }
}
