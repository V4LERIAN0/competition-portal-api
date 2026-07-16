package com.wodnsivar.competitionportal.auth.controller;

import com.wodnsivar.competitionportal.auth.dto.LoginRequest;
import com.wodnsivar.competitionportal.auth.dto.LoginResponse;
import com.wodnsivar.competitionportal.auth.dto.MeResponse;
import com.wodnsivar.competitionportal.auth.service.AuthService;
import com.wodnsivar.competitionportal.config.JwtConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.login(request);

        Cookie cookie = new Cookie(jwtConfig.getCookieName(), result.token());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // false for local dev. Set true in production HTTPS.
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtConfig.getExpirationMs() / 1000));

        response.addCookie(cookie);

        return result.response();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(jwtConfig.getCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return authService.getCurrentUser(authentication);
    }
}