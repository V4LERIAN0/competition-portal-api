package com.wodnsivar.competitionportal.auth.controller;

import com.wodnsivar.competitionportal.auth.dto.LoginRequest;
import com.wodnsivar.competitionportal.auth.dto.LoginResponse;
import com.wodnsivar.competitionportal.auth.dto.MeResponse;
import com.wodnsivar.competitionportal.auth.service.AuthService;
import com.wodnsivar.competitionportal.config.JwtConfig;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final JwtConfig jwtConfig;

  @PostMapping("/login")
  public LoginResponse login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    AuthService.AuthResult result = authService.login(request);

    addAuthCookie(response, result.token(), Duration.ofMillis(jwtConfig.getExpirationMs()));

    return result.response();
  }

  @PostMapping("/change-password")
  public LoginResponse changePassword(
      @Valid @RequestBody com.wodnsivar.competitionportal.auth.dto.ChangePasswordRequest request,
      HttpServletResponse response) {
    var result = authService.changePassword(request);
    addAuthCookie(response, result.token(), Duration.ofMillis(jwtConfig.getExpirationMs()));
    return result.response();
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletResponse response) {
    addAuthCookie(response, "", Duration.ZERO);
  }

  @GetMapping("/me")
  public MeResponse me(Authentication authentication) {
    return authService.getCurrentUser(authentication);
  }

  private void addAuthCookie(HttpServletResponse response, String value, Duration maxAge) {
    ResponseCookie cookie =
        ResponseCookie.from(jwtConfig.getCookieName(), value)
            .httpOnly(true)
            .secure(jwtConfig.isCookieSecure())
            .sameSite(jwtConfig.getCookieSameSite())
            .path("/")
            .maxAge(maxAge)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
