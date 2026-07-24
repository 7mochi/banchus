package pe.nanamochi.banchus.infrastructure.client

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.util.UriComponentsBuilder
import pe.nanamochi.banchus.beatmap.enums.BeatmapDirectDisplayMode
import pe.nanamochi.banchus.beatmap.enums.OsuDirectQuery

@Service
class OsuDirectApiClient(private val restTemplate: RestTemplate) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val baseUrl = "https://osu.direct/api"

    fun search(
        query: String,
        mode: Int,
        displayMode: BeatmapDirectDisplayMode,
        pageOffset: Int,
    ): String? {
        val builder =
            UriComponentsBuilder.fromUriString("$baseUrl/v2/search")
                .queryParam("amount", 100)
                .queryParam("offset", pageOffset * 100)
                .queryParam("osudirect", "true")

        val osuDirectQuery = OsuDirectQuery.fromQuery(query)
        if (osuDirectQuery != null) {
            builder.queryParam("sort", osuDirectQuery.sort)
        } else {
            builder.queryParam("q", query)
        }

        if (mode != -1) {
            builder.queryParam("mode", mode)
        }

        if (displayMode != BeatmapDirectDisplayMode.ALL) {
            builder.queryParam("status", displayMode.apiStatus)
        }

        log.debug(
            "osu!direct search request: query='{}', mode='{}', displayMode='{}', pageOffset={}",
            query,
            mode,
            displayMode,
            pageOffset,
        )

        return runCatching {
                restTemplate.getForObject<String>(builder.toUriString())?.let { formatResponse(it) }
            }
            .onFailure { e -> log.error("Error connecting to osu.direct API: ${e.message}") }
            .getOrNull()
    }

    private fun formatResponse(result: String): String {
        if (result.isBlank()) return result

        val parts = result.split(Regex("\\R"), 2)
        val firstLine = parts.getOrNull(0) ?: return result

        return runCatching {
                if (firstLine.toInt() == 100) {
                    "101\n${parts.getOrElse(1) { "" }}"
                } else {
                    result
                }
            }
            .getOrDefault(result)
    }
}
