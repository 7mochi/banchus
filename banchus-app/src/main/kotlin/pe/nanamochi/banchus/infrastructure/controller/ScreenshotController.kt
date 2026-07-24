package pe.nanamochi.banchus.infrastructure.controller

import com.github.michaelbull.result.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.infrastructure.security.AuthenticatedUser
import pe.nanamochi.banchus.core.service.StorageService

@RestController
class ScreenshotController(private val storageService: StorageService) {
    @GetMapping("/ss/{screenshotId}", produces = [MediaType.IMAGE_PNG_VALUE])
    fun getScreenshot(@PathVariable screenshotId: String): ResponseEntity<ByteArray> {
        val data =
            storageService
                .getScreenshot(screenshotId)
                .mapError { ResponseStatusException(HttpStatus.NOT_FOUND, "Screenshot not found.") }
                .getOrThrow { it }

        return ResponseEntity.ok().body(data)
    }

    @PostMapping("/web/osu-screenshot.php")
    fun uploadScreenshot(
        @AuthenticatedUser user: User,
        @RequestParam(value = "v", required = false) endpointVersion: Int?,
        @RequestParam(value = "ss", required = false) screenshotFile: MultipartFile?,
    ): ResponseEntity<String> {
        endpointVersion
            ?.takeIf { it != 1 }
            ?.let { log.warn("Incorrect endpoint version for {}: v{}", user.username, it) }

        val result = binding {
            val file =
                screenshotFile?.takeIf { !it.isEmpty }.toResultOr { HttpStatus.BAD_REQUEST }.bind()

            file
                .takeIf { it.size <= 4 * 1024 * 1024 }
                .toResultOr {
                    log.warn(
                        "User {} upload rejected: file too large ({} bytes)",
                        user.username,
                        file.size,
                    )
                    HttpStatus.CONTENT_TOO_LARGE
                }
                .bind()

            val filename =
                runCatching { file.bytes }
                    .mapError {
                        log.error(
                            "Failed to read bytes from multipart file for user {}",
                            user.username,
                        )
                        HttpStatus.INTERNAL_SERVER_ERROR
                    }
                    .andThen { bytes ->
                        storageService.saveScreenshot(bytes).mapError {
                            HttpStatus.INTERNAL_SERVER_ERROR
                        }
                    }
                    .bind()

            filename
        }

        return result.mapBoth(success = { ResponseEntity.ok(it) }, failure = { ResponseEntity(it) })
    }

    private val log = LoggerFactory.getLogger(javaClass)
}
