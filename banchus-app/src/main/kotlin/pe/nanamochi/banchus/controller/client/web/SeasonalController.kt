package pe.nanamochi.banchus.controller.client.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.config.BanchusProperties

@RestController
@RequestMapping("/web")
class SeasonalController(private val properties: BanchusProperties) {

    @GetMapping("/osu-getseasonal.php")
    fun getSeasonalBackgrounds(): List<String> = properties.seasonalBackgrounds.urls
}
