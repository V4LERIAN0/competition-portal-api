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
  private final com.wodnsivar.competitionportal.user.repository.UserAccountRepository accounts;
  private final org.springframework.security.crypto.password.PasswordEncoder encoder;

  public AuthResult login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(java.util.Locale.ROOT), request.password()));

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    String token = jwtService.generateToken(userPrincipal);

    LoginResponse response =
        new LoginResponse(
            userPrincipal.getId(),
            userPrincipal.getEmail(),
            userPrincipal.getRole(),
            userPrincipal.getLoginUsername(),
            userPrincipal.isMustChangePassword());

    return new AuthResult(token, response);
  }

  public MeResponse getCurrentUser(Authentication authentication) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
      throw new ForbiddenException("Authentication is required.");
    }

    return new MeResponse(
        userPrincipal.getId(),
        userPrincipal.getEmail(),
        userPrincipal.getRole(),
        userPrincipal.getLoginUsername(),
        userPrincipal.isMustChangePassword());
  }

  @org.springframework.transaction.annotation.Transactional
  public AuthResult changePassword(
      com.wodnsivar.competitionportal.auth.dto.ChangePasswordRequest request) {
    var principal =
        com.wodnsivar.competitionportal.auth.security.SecurityUtils.getCurrentUserOrThrow();
    var account =
        accounts
            .findForUpdate(principal.getId())
            .orElseThrow(() -> new ForbiddenException("Cuenta no disponible."));
    if (!Boolean.TRUE.equals(account.getEnabled())
        || account.getTokenVersion() != principal.getTokenVersion()
        || !encoder.matches(request.currentPassword(), account.getPasswordHash())) {
      throw new ForbiddenException("La contraseña actual no es correcta o la sesión ha vencido.");
    }
    if (request.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
        || encoder.matches(request.newPassword(), account.getPasswordHash())) {
      throw new com.wodnsivar.competitionportal.common.exception.BadRequestException(
          "Usa una contraseña nueva, de 12 a 72 caracteres (máximo 72 bytes).");
    }
    account.setPasswordHash(encoder.encode(request.newPassword()));
    account.setMustChangePassword(false);
    account.setTokenVersion(account.getTokenVersion() + 1);
    accounts.saveAndFlush(account);
    var updated = new UserPrincipal(account);
    return new AuthResult(
        jwtService.generateToken(updated),
        new LoginResponse(
            updated.getId(),
            updated.getEmail(),
            updated.getRole(),
            updated.getLoginUsername(),
            false));
  }

  public record AuthResult(String token, LoginResponse response) {}
}
