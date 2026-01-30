package pe.nanamochi.banchus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FilterTest extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (request.getRequestURI().contains("osu-submit-modular-selector.php")) {
      System.out.println("SUBMIT FILTER HIT");
      System.out.println("URI: " + request.getRequestURI());
      System.out.println("Content-Type: " + request.getContentType());
      System.out.println("Headers:");
      Collections.list(request.getHeaderNames())
          .forEach(h -> System.out.println("  " + h + " = " + request.getHeader(h)));
      int size =
          Collections.list(request.getHeaderNames()).stream()
              .mapToInt(h -> h.length() + request.getHeader(h).length())
              .sum();

      System.out.println("Header size ~ " + size + " bytes");
    }

    filterChain.doFilter(request, response);
  }
}
