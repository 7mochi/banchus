package pe.nanamochi.banchus.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.domain.enums.BeatmapDirectDisplayMode

@Component
class DirectDisplayModeConverter : Converter<String, BeatmapDirectDisplayMode> {
    override fun convert(source: String): BeatmapDirectDisplayMode {
        val value = source.toIntOrNull() ?: return BeatmapDirectDisplayMode.ALL
        return BeatmapDirectDisplayMode.fromValue(value)
    }
}
