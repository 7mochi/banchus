package pe.nanamochi.banchus.dto.external

import com.fasterxml.jackson.annotation.JsonProperty

data class Geolocation(
    val status: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val region: String? = null,
    val regionName: String? = null,
    val city: String? = null,
    val zip: String? = null,
    @JsonProperty("lat") val latitude: Float = 0.0f,
    @JsonProperty("lon") val longitude: Float = 0.0f,
    val timezone: String? = null,
    val isp: String? = null,
    val org: String? = null,
    val query: String? = null,
) {
    companion object {
        fun local() = Geolocation(status = "success", countryCode = "XX", country = "Local")
    }
}
