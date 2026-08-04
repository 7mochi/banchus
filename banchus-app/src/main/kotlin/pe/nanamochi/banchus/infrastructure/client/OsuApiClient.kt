package pe.nanamochi.banchus.infrastructure.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import org.springframework.web.util.UriComponentsBuilder
import pe.nanamochi.banchus.beatmap.dto.external.OsuApiBeatmap
import pe.nanamochi.banchus.infrastructure.config.BanchusProperties

@Service
class OsuApiClient(
    private val restTemplate: RestTemplate,
    private val properties: BanchusProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val baseUrl = "https://osu.ppy.sh"

    @CircuitBreaker(name = "osuApi", fallbackMethod = "fallbackOsuFile")
    @Retry(name = "osuApi")
    @RateLimiter(name = "osuApi")
    fun fetchOsuFile(beatmapId: Int): ByteArray? {
        val response = restTemplate.getForEntity<ByteArray>("$baseUrl/osu/$beatmapId")
        if (response.statusCode.is2xxSuccessful) return response.body
        if (response.statusCode.value() in setOf(404, 451)) return null
        throw OsuApiException("HTTP ${response.statusCode} downloading .osu for beatmap $beatmapId")
    }

    fun fallbackOsuFile(beatmapId: Int, ex: Exception): ByteArray? {
        log.error("Failed to download .osu file for beatmap $beatmapId: ${ex.message}")
        throw OsuApiUnavailableException("osu! API unavailable for beatmap $beatmapId", ex)
    }

    @CircuitBreaker(name = "osuApi", fallbackMethod = "fallbackBeatmapSearch")
    @Retry(name = "osuApi")
    @RateLimiter(name = "osuApi")
    fun fetchBeatmapByMd5(beatmapMd5: String): OsuApiBeatmap? {
        val results = callApi("h", beatmapMd5)
        return results.firstOrNull()
    }

    @CircuitBreaker(name = "osuApi", fallbackMethod = "fallbackBeatmapSearch")
    @Retry(name = "osuApi")
    @RateLimiter(name = "osuApi")
    fun fetchBeatmapById(beatmapId: Int): OsuApiBeatmap? {
        val results = callApi("b", beatmapId.toString())
        return results.firstOrNull()
    }

    private fun callApi(paramName: String, paramValue: String): List<OsuApiBeatmap> {
        val url =
            UriComponentsBuilder.fromUriString("$baseUrl/api/get_beatmaps")
                .queryParam(paramName, paramValue)
                .queryParam("k", apiKey())
                .toUriString()

        val response = restTemplate.getForEntity<Array<OsuApiBeatmap>>(url)

        if (!response.statusCode.is2xxSuccessful) {
            if (response.statusCode.value() in setOf(404, 451)) return emptyList()
            throw OsuApiException(
                "osu! API returned ${response.statusCode} for $paramName=$paramValue"
            )
        }

        return response.body?.toList() ?: emptyList()
    }

    private fun apiKey(): String {
        val keys = properties.osuApi.v1.keys
        return if (keys.isNotEmpty()) keys.random() else ""
    }

    fun fallbackBeatmapSearch(ex: Exception): OsuApiBeatmap? {
        log.error("osu! API unavailable for search: ${ex.message}")
        throw OsuApiUnavailableException("osu! API unavailable for search", ex)
    }
}

class OsuApiException(message: String) : RuntimeException(message)

class OsuApiUnavailableException(message: String, cause: Throwable) :
    RuntimeException(message, cause)
