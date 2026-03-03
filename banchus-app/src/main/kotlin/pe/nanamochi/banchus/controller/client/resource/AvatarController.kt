package pe.nanamochi.banchus.controller.client.resource

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

@RestController
@RequestMapping("/")
class AvatarController(private val storageService: StorageService) {
    @GetMapping("/{userId}", produces = [MediaType.IMAGE_PNG_VALUE])
    fun getAvatar(@PathVariable userId: String): ResponseEntity<ByteArray> {
        val bytes =
            storageService
                .getAvatar(userId)
                .mapError { ResponseStatusException(HttpStatus.NOT_FOUND) }
                .getOrThrow { it }

        return ResponseEntity.ok().body(bytes)
    }
}
