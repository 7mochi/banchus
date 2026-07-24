package pe.nanamochi.banchus.auth.controller

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
import pe.nanamochi.banchus.auth.broadcast.LoginBroadcaster
import pe.nanamochi.banchus.auth.service.BanchoService
import pe.nanamochi.banchus.auth.service.LoginService
import pe.nanamochi.banchus.core.error.SessionExpired

@RestController
@RequestMapping("/")
class BanchoController(
    private val banchoService: BanchoService,
    private val loginService: LoginService,
    private val loginBroadcaster: LoginBroadcaster,
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
        val loginResult = loginService.handleLogin(rawData, headers)
        val loginResponse =
            loginResult
                .map { loginBroadcaster.loginSuccess(it) }
                .getOrElse { error -> loginBroadcaster.loginFailure(error) }
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
                when (error) {
                    is SessionExpired ->
                        ResponseEntity.ok()
                            .header("cho-token", "no")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(error.payload)
                    else -> {
                        log.warn("Bancho request failed for token {}: {}", token, error)

                        ResponseEntity.ok()
                            .header("cho-token", "no")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(ByteArray(0))
                    }
                }
            }
    }
}
