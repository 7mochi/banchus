package pe.nanamochi.banchus.controller.resource

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.dto.MenuContentResponse

@RestController
@RequestMapping("/")
class MenuController(
    @Value("\${banchus.menu-icon.image-url}") private val menuIconImage: String,
    @Value("\${banchus.menu-icon.redirect-url}") private val menuIconUrl: String,
) {
    @GetMapping("/menu-content.json")
    fun getMenuContent() =
        MenuContentResponse(
            images = listOf(MenuContentResponse.MenuImage(image = menuIconImage, url = menuIconUrl))
        )
}
