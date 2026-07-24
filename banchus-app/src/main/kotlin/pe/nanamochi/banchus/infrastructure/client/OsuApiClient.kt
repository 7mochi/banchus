package pe.nanamochi.banchus.infrastructure.client

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
    private val apiKey: String = properties.osuApi.v1.key

    fun getOsuFile(beatmapId: Int): ByteArray? {
        return runCatching {
                val response = restTemplate.getForEntity<ByteArray>("$baseUrl/osu/$beatmapId")
                if (response.statusCode.is2xxSuccessful) response.body else null
            }
            .onFailure { e ->
                log.error("Error downloading .osu file for beatmap $beatmapId: ${e.message}")
            }
            .getOrNull()
    }

    fun getBeatmap(beatmapMd5: String): OsuApiBeatmap? = callApi("h", beatmapMd5).firstOrNull()

    fun getBeatmap(beatmapId: Int): OsuApiBeatmap? =
        callApi("b", beatmapId.toString()).firstOrNull()

    fun getBeatmaps(beatmapSetId: Int): List<OsuApiBeatmap> = callApi("s", beatmapSetId.toString())

    private fun callApi(paramName: String, paramValue: String): List<OsuApiBeatmap> {
        return runCatching {
                val url =
                    UriComponentsBuilder.fromUriString("$baseUrl/api/get_beatmaps")
                        .queryParam(paramName, paramValue)
                        .queryParam("k", apiKey)
                        .toUriString()

                val response = restTemplate.getForEntity<Array<OsuApiBeatmap>>(url)

                response.body?.toList() ?: emptyList()
            }
            .onFailure { e ->
                log.error("Error calling osu!api ($paramName=$paramValue): ${e.message}")
            }
            .getOrDefault(emptyList())
    }
}
