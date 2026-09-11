package com.wodnsivar.competitionportal.auth.security;

import com.wodnsivar.competitionportal.auth.service.CustomUserDetailsService;
import com.wodnsivar.competitionportal.auth.service.JwtService;
import com.wodnsivar.competitionportal.config.JwtConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final JwtConfig jwtConfig;
  private final CustomUserDetailsService customUserDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractTokenFromCookies(request);

    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String email = jwtService.extractEmail(token);

      if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserPrincipal userPrincipal =
            (UserPrincipal) customUserDetailsService.loadUserByUsername(email);

        if (jwtService.isTokenValid(token, userPrincipal)) {
          UsernamePasswordAuthenticationToken authenticationToken =
              new UsernamePasswordAuthenticationToken(
                  userPrincipal, null, userPrincipal.getAuthorities());

          authenticationToken.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
      }
    } catch (Exception ignored) {
      SecurityContextHolder.clearContext();
    }

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (authentication != null
        && authentication.getPrincipal() instanceof UserPrincipal principal
        && principal.isMustChangePassword()
        && !"OPTIONS".equals(request.getMethod())
        && !path.startsWith("/api/public/")
        && !java.util.Set.of(
                "/api/auth/me", "/api/auth/logout", "/api/auth/login", "/api/auth/change-password")
            .contains(path)) {
      response.setStatus(403);
      response.setContentType("application/json;charset=UTF-8");
      response
          .getWriter()
          .write(
              "{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Debes cambiar tu contraseña"
                  + " antes de continuar.\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }

  private String extractTokenFromCookies(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }

    for (Cookie cookie : request.getCookies()) {
      if (jwtConfig.getCookieName().equals(cookie.getName())) {
        return cookie.getValue();
      }
    }

    return null;
  }
}
