package pe.nanamochi.banchus.controller.bancho

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.service.BanchoService
import pe.nanamochi.banchus.service.LoginService

@RestController
@RequestMapping("/")
class BanchoController(
    private val loginService: LoginService,
    private val banchoService: BanchoService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun banchoHandler(
        @RequestHeader headers: HttpHeaders,
        @RequestBody(required = false) body: ByteArray?,
    ): ResponseEntity<ByteArray> {
        val requestBody = body ?: ByteArray(0)

        return headers.getFirst("osu-token")?.let { token ->
            handleBanchoRequest(token, requestBody)
        } ?: handleLoginRequest(headers, requestBody)
    }

    private fun handleLoginRequest(
        headers: HttpHeaders,
        body: ByteArray,
    ): ResponseEntity<ByteArray> {
        val rawData = String(body, Charsets.UTF_8)
        val loginResponse = loginService.handleLogin(rawData, headers)

        return ResponseEntity.ok()
            .header("cho-token", loginResponse.token)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(loginResponse.payload)
    }

    private fun handleBanchoRequest(token: String, body: ByteArray): ResponseEntity<ByteArray> {
        return banchoService
            .handlePackets(token, body)
            .map { responsePayload ->
                ResponseEntity.ok()
                    .header("cho-token", token)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(responsePayload)
            }
            .getOrElse { error ->
                log.warn("Bancho request failed for token {}: {}", token, error)

                ResponseEntity.ok()
                    .header("cho-token", "no")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(ByteArray(0))
            }
    }
}
