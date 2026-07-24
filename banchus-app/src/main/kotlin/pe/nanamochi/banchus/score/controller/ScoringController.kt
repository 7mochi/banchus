package pe.nanamochi.banchus.score.controller

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
import pe.nanamochi.banchus.score.service.ScoreService
import pe.nanamochi.banchus.core.error.BeatmapNotFound
import pe.nanamochi.banchus.core.error.InvalidCredentials

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
        return scoreService
            .submitScore(request, headers, ivB64, clientHashB64, scoreTime, passwordMd5, osuVersion)
            .mapBoth(
                success = { chartString -> chartString },
                failure = { domainError ->
                    when (domainError) {
                        is BeatmapNotFound -> "error: beatmap"
                        is InvalidCredentials -> ""
                        else -> "error: no"
                    }
                },
            )
    }
}
