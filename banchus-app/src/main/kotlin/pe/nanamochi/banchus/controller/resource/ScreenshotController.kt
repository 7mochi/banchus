package pe.nanamochi.banchus.controller.resource

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.mapError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pe.nanamochi.banchus.service.StorageService

@RestController("ResourceScreenshotController")
@RequestMapping("/ss")
class ScreenshotController(private val storageService: StorageService) {
    @GetMapping("/{screenshotId}", produces = [MediaType.IMAGE_PNG_VALUE])
    fun getScreenshot(@PathVariable screenshotId: String): ResponseEntity<ByteArray> {
        val data =
            storageService
                .getScreenshot(screenshotId)
                .mapError { ResponseStatusException(HttpStatus.NOT_FOUND, "Screenshot not found.") }
                .getOrThrow { it }

        return ResponseEntity.ok().body(data)
    }
}
