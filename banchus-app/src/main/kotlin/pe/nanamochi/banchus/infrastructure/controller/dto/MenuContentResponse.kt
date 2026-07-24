package pe.nanamochi.banchus.infrastructure.controller.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class MenuContentResponse(val images: List<MenuImage>) {
    data class MenuImage(
        val image: String,
        val url: String,
        @get:JsonProperty("IsCurrent") val isCurrent: Boolean = true,
        val begins: String? = null,
        val expires: String? = null,
    )
}
