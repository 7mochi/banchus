package pe.nanamochi.banchus.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This filter intercepts requests to /web/osu-submit-modular-selector.php
 * BEFORE Spring tries to parse the multipart.
 * 
 * Instead of letting the request reach Spring's DispatcherServlet
 * (which would cause Spring to try parsing the multipart),
 * this filter does a direct FORWARD to the custom servlet,
 * bypassing Spring completely.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ScoreSubmissionFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(ScoreSubmissionFilter.class);

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, java.io.IOException {
    
    if (!(request instanceof HttpServletRequest)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getRequestURI();
    String method = httpRequest.getMethod();
    String queryString = httpRequest.getQueryString() != null ? "?" + httpRequest.getQueryString() : "";
    
    // Log ALL requests to /web/*
    if (path.contains("/web/")) {
      if (httpRequest.getQueryString() != null && !httpRequest.getQueryString().isEmpty()) {
        String[] params = httpRequest.getQueryString().split("&");
        StringBuilder paramLog = new StringBuilder();
        if (params.length > 0) {
          for (String param : params) {
            paramLog.append("\n  Param: ");
            if (param.length() > 100) {
              paramLog.append(param, 0, 100).append("...");
            } else {
              paramLog.append(param);
            }
          }
        }
        logger.info("[{}] {} | Query: {} | UA: {}{}", 
            method, path, queryString, httpRequest.getHeader("User-Agent"), paramLog);
      } else {
        logger.info("[{}] {} | Query: {} | UA: {}", 
            method, path, queryString, httpRequest.getHeader("User-Agent"));
      }
    }
    
    // If request is for score submission endpoint, handle it directly WITHOUT going through Spring's DispatcherServlet
    if (path.contains("osu-submit-modular-selector.php") && "POST".equals(method)) {
      logger.info("🔗 Routing score submission to servlet, bypassing Spring multipart parsing");
      // Forward directly to the servlet, bypassing DispatcherServlet which tries to parse multipart
      request.getRequestDispatcher("/web/osu-submit-modular-selector.php").forward(request, response);
      return;
    }

    // For all other requests, process normally with Spring
    chain.doFilter(request, response);
  }
}
