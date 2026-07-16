package com.wodnsivar.competitionportal.auth.service;

import com.wodnsivar.competitionportal.auth.dto.LoginRequest;
import com.wodnsivar.competitionportal.auth.dto.LoginResponse;
import com.wodnsivar.competitionportal.auth.dto.MeResponse;
import com.wodnsivar.competitionportal.auth.security.UserPrincipal;
import com.wodnsivar.competitionportal.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.password()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(userPrincipal);

        LoginResponse response = new LoginResponse(
                userPrincipal.getId(),
                userPrincipal.getEmail(),
                userPrincipal.getRole()
        );

        return new AuthResult(token, response);
    }

    public MeResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ForbiddenException("Authentication is required.");
        }

        return new MeResponse(
                userPrincipal.getId(),
                userPrincipal.getEmail(),
                userPrincipal.getRole()
        );
    }

    public record AuthResult(
            String token,
            LoginResponse response
    ) {
    }
}