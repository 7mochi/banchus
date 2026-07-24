package pe.nanamochi.banchus.identity.service

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.runCatching
import java.net.InetAddress
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.identity.dto.Geolocation
import pe.nanamochi.banchus.infrastructure.client.IPApiClient
import pe.nanamochi.banchus.core.error.GeolocationError
import pe.nanamochi.banchus.core.error.ResolutionFailed

@Service
class GeolocationService(private val ipApiClient: IPApiClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun resolve(headers: HttpHeaders): Pair<InetAddress, Geolocation> {
        val ipRaw = resolveIp(headers) ?: "127.0.0.1"
        val ipAddress = InetAddress.getByName(ipRaw)

        val geolocation =
            when {
                ipAddress.isLoopbackAddress || ipAddress.isSiteLocalAddress -> {
                    log.debug(
                        "Local IP detected ({}). Skipping external geolocation resolution.",
                        ipRaw,
                    )
                    Geolocation.local()
                }
                else ->
                    fetchFromHeaders(headers)
                        .orElse {
                            log.info(
                                "No geolocation headers found. Falling back to IP-API for IP: {}",
                                ipRaw,
                            )
                            Ok(ipApiClient.fetchFromIP(ipAddress))
                        }
                        .getOrElse { Geolocation.local() }
            }

        return ipAddress to geolocation
    }

    private fun resolveIp(headers: HttpHeaders): String? =
        listOf("CF-Connecting-IP", "X-Forwarded-For", "X-Real-IP").firstNotNullOfOrNull { header ->
            headers
                .getFirst(header)
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.also { log.debug("IP resolved as '{}' via header '{}'", it, header) }
        }

    private fun fetchFromHeaders(headers: HttpHeaders): Result<Geolocation, GeolocationError> =
        fetchCloudflare(headers).orElse { fetchNginx(headers) }

    private fun fetchCloudflare(h: HttpHeaders): Result<Geolocation, GeolocationError> =
        runCatching {
                Geolocation(
                    status = "success",
                    countryCode = h.getFirst("CF-IPCountry")!!,
                    latitude = h.getFirst("CF-IPLatitude")!!.toFloat(),
                    longitude = h.getFirst("CF-IPLongitude")!!.toFloat(),
                )
            }
            .mapError { ResolutionFailed }
            .onSuccess { log.info("Geolocation resolved via Cloudflare headers.") }
            .onFailure { log.debug("Cloudflare headers resolution failed: {}", it) }

    private fun fetchNginx(h: HttpHeaders): Result<Geolocation, GeolocationError> =
        runCatching {
                Geolocation(
                    status = "success",
                    countryCode = h.getFirst("X-Country-Code")!!,
                    latitude = h.getFirst("X-Latitude")!!.toFloat(),
                    longitude = h.getFirst("X-Longitude")!!.toFloat(),
                )
            }
            .mapError { ResolutionFailed }
            .onSuccess { log.info("Geolocation resolved via Nginx headers.") }
            .onFailure { log.debug("Nginx headers resolution failed: {}", it) }
}
