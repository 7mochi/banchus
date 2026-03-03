package pe.nanamochi.banchus.controller.client.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/web")
class SeasonalController(
    @Value($$"${banchus.seasonal-backgrounds.urls}")
    private val seasonalBackgroundsUrls: List<String>
) {

    @GetMapping("/osu-getseasonal.php")
    fun getSeasonalBackgrounds(): List<String> = seasonalBackgroundsUrls
}
