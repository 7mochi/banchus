package pe.nanamochi.banchus.infrastructure.clients

import java.net.InetAddress
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import pe.nanamochi.banchus.dto.external.Geolocation

@Service
class IPApiClient(private val restTemplate: RestTemplate) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchFromIP(ip: InetAddress): Geolocation {
        val hostAddress = ip.hostAddress
        val url = "http://ip-api.com/json/$hostAddress"

        return runCatching { restTemplate.getForObject<Geolocation>(url) }
            .getOrNull()
            ?.takeIf { it.status == "success" }
            ?: run {
                log.warn(
                    "IP-API failed or returned non-success for IP: $hostAddress - defaulting to local geolocation."
                )
                Geolocation.local()
            }
    }
}
