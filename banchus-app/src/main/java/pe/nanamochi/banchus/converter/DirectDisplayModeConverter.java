package pe.nanamochi.banchus.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.domain.enums.BeatmapDirectDisplayMode;

@Component
public class DirectDisplayModeConverter implements Converter<String, BeatmapDirectDisplayMode> {

  @Override
  public BeatmapDirectDisplayMode convert(String source) {
    try {
      int value = Integer.parseInt(source);
      return BeatmapDirectDisplayMode.fromValue(value);
    } catch (IllegalArgumentException e) {
      return BeatmapDirectDisplayMode.ALL;
    }
  }
}
