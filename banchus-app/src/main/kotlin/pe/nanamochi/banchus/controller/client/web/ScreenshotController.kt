package pe.nanamochi.banchus.controller.client.web

import com.github.michaelbull.result.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.infrastructure.security.AuthenticatedUser
import pe.nanamochi.banchus.service.StorageService

@RestController
@RequestMapping("/web")
class ScreenshotController(private val storageService: StorageService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/osu-screenshot.php")
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
}
