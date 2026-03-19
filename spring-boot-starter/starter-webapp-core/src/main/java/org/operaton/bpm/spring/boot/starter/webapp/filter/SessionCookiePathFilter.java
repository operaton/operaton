package org.operaton.bpm.spring.boot.starter.webapp.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionCookiePathFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(SessionCookiePathFilter.class);
  public static final String PARAM_SESSION_COOKIE_NAME = "sessionCookieName";
  public static final String PARAM_COOKIE_PATH = "cookiePath";
  public static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";
  public static final String DEFAULT_COOKIE_PATH = "/operaton";

  private String sessionCookieName = DEFAULT_SESSION_COOKIE_NAME;
  private String cookiePath = DEFAULT_COOKIE_PATH;

  @Override
  public void init(FilterConfig filterConfig) {
    String paramName = filterConfig.getInitParameter(PARAM_SESSION_COOKIE_NAME);
    if (paramName != null && !paramName.isBlank()) {
      sessionCookieName = paramName;
    }
    String paramPath = filterConfig.getInitParameter(PARAM_COOKIE_PATH);
    if (paramPath != null && !paramPath.isBlank()) {
      cookiePath = paramPath;
    }

    log.debug("SessionCookiePathFilter initialized: sessionCookieName='{}', cookiePath='{}'",
            sessionCookieName, cookiePath);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    if (response instanceof HttpServletResponse httpResponse) {
      chain.doFilter(request, new SessionCookiePathResponseWrapper(httpResponse));
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() { }

  class SessionCookiePathResponseWrapper extends HttpServletResponseWrapper {

    SessionCookiePathResponseWrapper(HttpServletResponse response) {
      super(response);
    }

    @Override
    public void addCookie(Cookie cookie) {
      if (sessionCookieName.equals(cookie.getName())) {
        log.trace("Rewriting Path of cookie '{}' from '{}' to '{}'",
                cookie.getName(), cookie.getPath(), cookiePath);
        cookie.setPath(cookiePath);
      }
      super.addCookie(cookie);
    }

    @Override
    public void addHeader(String name, String value) {
      super.addHeader(name, rewriteSetCookieHeader(name, value));
    }

    @Override
    public void setHeader(String name, String value) {
      super.setHeader(name, rewriteSetCookieHeader(name, value));
    }

    private String rewriteSetCookieHeader(String name, String value) {
      if (!"Set-Cookie".equalsIgnoreCase(name)) {
        return value;
      }
      if (value == null || !value.startsWith(sessionCookieName + "=")) {
        return value;
      }

      log.trace("Rewriting Set-Cookie header for '{}': original='{}'", sessionCookieName, value);

      String rewritten = value.replaceAll("(?i);\\s*Path=[^;]*", "");
      rewritten = rewritten + "; Path=" + cookiePath;

      log.trace("Rewritten Set-Cookie header: '{}'", rewritten);
      return rewritten;
    }
  }
}