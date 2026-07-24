package pe.nanamochi.banchus.infrastructure.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pe.nanamochi.banchus.infrastructure.config.BanchusProperties

@RestController
@RequestMapping("/web")
class SeasonalController(private val properties: BanchusProperties) {

    @GetMapping("/osu-getseasonal.php")
    fun getSeasonalBackgrounds(): List<String> = properties.seasonalBackgrounds.urls
}
