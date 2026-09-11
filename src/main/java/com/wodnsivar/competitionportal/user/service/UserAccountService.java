package com.wodnsivar.competitionportal.user.service;

import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.common.exception.*;
import com.wodnsivar.competitionportal.enums.*;
import com.wodnsivar.competitionportal.user.dto.*;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import com.wodnsivar.competitionportal.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAccountService {
  private final CompetitionAthleteRepository athletes;
  private final UserAccountRepository accounts;
  private final PasswordEncoder encoder;
  private final EntityManager entityManager;

  @Transactional(readOnly = true)
  public List<AthleteAccessResponse> preview(Long competitionId) {
    Set<String> reserved = new HashSet<>();
    return athletes.findByCompetitionIdOrderByFullNameAsc(competitionId).stream()
        .filter(
            a ->
                a.getStatus() != AthleteStatus.WITHDRAWN
                    && a.getStatus() != AthleteStatus.DISQUALIFIED)
        .map(
            a -> {
              UserAccount account = a.getUserAccount();
              String username = account == null ? null : account.getUsername();
              boolean configured = username != null;
              if (!configured) {
                String base = proposedUsername(a.getFullName());
                username = base;
                int suffix = 2;
                while (reserved.contains(username) || accounts.existsByUsernameIgnoreCase(username))
                  username = base + "." + suffix++;
              }
              reserved.add(username);
              return new AthleteAccessResponse(
                  a.getId(),
                  a.getFullName(),
                  a.getCategory().getName(),
                  username,
                  configured,
                  account != null && account.isMustChangePassword(),
                  account != null && Boolean.TRUE.equals(account.getEnabled()));
            })
        .toList();
  }

  public List<AthleteAccessResponse> provision(Long competitionId, AthleteAccessRequest request) {
    if (!request.useNameBasedPassword()) validateTemporaryPassword(request.temporaryPassword());
    // Serialize activation for this competition, including concurrent admin clicks.
    var competition =
        entityManager.find(
            com.wodnsivar.competitionportal.competition.entity.Competition.class,
            competitionId,
            LockModeType.PESSIMISTIC_WRITE);
    if (competition == null) throw new ResourceNotFoundException("Competencia no encontrada.");
    Set<Long> selected = new HashSet<>();
    for (var entry : request.athletes()) {
      if (!selected.add(entry.athleteId()))
        throw new BadRequestException("Atleta duplicado en la selección.");
      var athlete =
          athletes
              .findById(entry.athleteId())
              .orElseThrow(() -> new ResourceNotFoundException("Atleta no encontrado."));
      if (!athlete.getCompetition().getId().equals(competitionId))
        throw new BadRequestException("El atleta pertenece a otra competencia.");
      if (athlete.getStatus() == AthleteStatus.WITHDRAWN
          || athlete.getStatus() == AthleteStatus.DISQUALIFIED)
        throw new ConflictException("No se puede activar un atleta retirado o descalificado.");
      if (athlete.getUserAccount() != null && athlete.getUserAccount().getUsername() != null)
        continue;
      activate(athlete, entry.username(), initialPassword(athlete, request));
    }
    entityManager.flush();
    return preview(competitionId);
  }

  public void reset(Long athleteId, AthleteAccessRequest request) {
    if (!request.useNameBasedPassword()) validateTemporaryPassword(request.temporaryPassword());
    if (request.athletes().size() != 1
        || !request.athletes().getFirst().athleteId().equals(athleteId))
      throw new BadRequestException("Selecciona una sola cuenta para restablecer.");
    var athlete =
        entityManager.find(CompetitionAthlete.class, athleteId, LockModeType.PESSIMISTIC_WRITE);
    if (athlete == null) throw new ResourceNotFoundException("Atleta no encontrado.");
    activate(athlete, request.athletes().getFirst().username(), initialPassword(athlete, request));
  }

  private void activate(CompetitionAthlete athlete, String username, String password) {
    UserAccount account = athlete.getUserAccount();
    if (account != null) {
      account = accounts.findForUpdate(account.getId()).orElseThrow();
      if (account.getRole() != UserRole.ATHLETE)
        throw new ConflictException("Esta cuenta no es de atleta.");
    }
    var duplicate = accounts.findByUsernameIgnoreCase(username);
    if (duplicate.isPresent()
        && (account == null || !duplicate.get().getId().equals(account.getId())))
      throw new ConflictException("El usuario " + username + " ya existe. Revisa la selección.");
    if (account == null) {
      // Internal identifier only; never treated as a verified contact email.
      String email = "athlete." + athlete.getId() + "@accounts.sivarfest.fit";
      if (accounts.existsByEmail(email))
        throw new ConflictException("Ya existe una cuenta sin vincular para este atleta.");
      account = UserAccount.builder().email(email).role(UserRole.ATHLETE).enabled(true).build();
    }
    account.setUsername(username);
    account.setPasswordHash(encoder.encode(password));
    account.setMustChangePassword(true);
    account.setTokenVersion(account.getTokenVersion() + 1);
    account.setEnabled(true);
    athlete.setUserAccount(accounts.saveAndFlush(account));
    athletes.save(athlete);
  }

  private String initialPassword(CompetitionAthlete athlete, AthleteAccessRequest request) {
    return request.useNameBasedPassword()
        ? proposedUsername(athlete.getFullName()).split("\\.")[0] + "sf!"
        : request.temporaryPassword();
  }

  private void validateTemporaryPassword(String password) {
    if (password == null || password.length() < 4)
      throw new BadRequestException("Escribe una contraseña temporal de al menos 4 caracteres.");
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new BadRequestException("La contraseña temporal supera 72 bytes.");
  }

  public static String proposedUsername(String fullName) {
    String cleaned =
        Normalizer.normalize(fullName, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9 ]", " ")
            .trim();
    String[] words = cleaned.split("\\s+");
    String first = words.length == 0 || words[0].isBlank() ? "atleta" : words[0];
    String last = words.length > 1 ? words[words.length - 1] : "sf";
    return first.substring(0, Math.min(40, first.length()))
        + "."
        + last.substring(0, Math.min(40, last.length()));
  }
}
