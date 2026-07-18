package pe.nanamochi.banchus.controller.client.resource

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.config.BanchusProperties
import pe.nanamochi.banchus.dto.client.MenuContentResponse

@RestController
@RequestMapping("/")
class MenuController(private val properties: BanchusProperties) {
    @GetMapping("/menu-content.json")
    fun getMenuContent() =
        MenuContentResponse(
            images =
                listOf(
                    MenuContentResponse.MenuImage(
                        image = properties.menuIcon.imageUrl,
                        url = properties.menuIcon.redirectUrl,
                    )
                )
        )
}
