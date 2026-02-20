package pe.nanamochi.banchus.controller.resource;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nanamochi.banchus.domain.dto.MenuContentResponse;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class MenuController {
  @Value("${banchus.menu-icon.image-url}")
  private String menuIconImage;

  @Value("${banchus.menu-icon.redirect-url}")
  private String menuIconUrl;

  @GetMapping(value = "/menu-content.json")
  public MenuContentResponse getMenuContent() {
    MenuContentResponse.MenuImage icon =
        new MenuContentResponse.MenuImage(menuIconImage, menuIconUrl, true, null, null);
    return new MenuContentResponse(List.of(icon));
  }
}
