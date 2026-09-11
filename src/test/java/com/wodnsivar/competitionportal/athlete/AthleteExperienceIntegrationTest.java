package com.wodnsivar.competitionportal.athlete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wodnsivar.competitionportal.athlete.entity.CompetitionAthlete;
import com.wodnsivar.competitionportal.athlete.repository.CompetitionAthleteRepository;
import com.wodnsivar.competitionportal.auth.security.UserPrincipal;
import com.wodnsivar.competitionportal.auth.service.JwtService;
import com.wodnsivar.competitionportal.category.entity.CompetitionCategory;
import com.wodnsivar.competitionportal.category.repository.CompetitionCategoryRepository;
import com.wodnsivar.competitionportal.competition.entity.Competition;
import com.wodnsivar.competitionportal.competition.repository.CompetitionRepository;
import com.wodnsivar.competitionportal.enums.*;
import com.wodnsivar.competitionportal.event.entity.CompetitionEvent;
import com.wodnsivar.competitionportal.event.repository.CompetitionEventRepository;
import com.wodnsivar.competitionportal.heat.entity.*;
import com.wodnsivar.competitionportal.heat.repository.*;
import com.wodnsivar.competitionportal.judge.entity.*;
import com.wodnsivar.competitionportal.judge.repository.*;
import com.wodnsivar.competitionportal.user.dto.AthleteAccessRequest;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import com.wodnsivar.competitionportal.user.repository.UserAccountRepository;
import com.wodnsivar.competitionportal.user.service.UserAccountService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.athlete-photo-directory=${java.io.tmpdir}/sivarfest-test-photos")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AthleteExperienceIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JwtService jwt;
  @Autowired PasswordEncoder encoder;
  @Autowired CompetitionRepository competitions;
  @Autowired CompetitionCategoryRepository categories;
  @Autowired CompetitionAthleteRepository athletes;
  @Autowired CompetitionEventRepository events;
  @Autowired CompetitionHeatRepository heats;
  @Autowired CompetitionHeatAthleteRepository positions;
  @Autowired UserAccountRepository accounts;
  @Autowired UserAccountService access;
  @Autowired CompetitionJudgeRepository judges;
  @Autowired CompetitionJudgeAssignmentRepository assignments;
  @Autowired Clock competitionClock;
  Competition competition;
  CompetitionCategory category;
  CompetitionAthlete athlete, other;
  CompetitionEvent event;
  CompetitionHeat heat;
  CompetitionHeatAthlete position, otherPosition;
  UserAccount admin;

  @BeforeEach
  void setup() {
    competition =
        competitions.saveAndFlush(
            Competition.builder()
                .name("SIVARFEST Test")
                .slug("experience-test")
                .registrationStatus(RegistrationStatus.CLOSED)
                .visibilityStatus(VisibilityStatus.PUBLIC)
                .status(CompetitionStatus.PUBLISHED)
                .checkInOpenMinutesBeforeHeat(30)
                .build());
    category =
        categories.saveAndFlush(
            CompetitionCategory.builder()
                .competition(competition)
                .name("Female SC")
                .genderClassification(GenderClassification.FEMALE)
                .divisionLabel("SC")
                .active(true)
                .displayOrder(1)
                .build());
    athlete =
        athletes.saveAndFlush(
            CompetitionAthlete.builder()
                .competition(competition)
                .category(category)
                .fullName("María López")
                .height(new BigDecimal("165"))
                .weight(new BigDecimal("60"))
                .email("private@example.test")
                .phoneNumber("private-phone")
                .status(AthleteStatus.CONFIRMED)
                .checkedIn(false)
                .build());
    other =
        athletes.saveAndFlush(
            CompetitionAthlete.builder()
                .competition(competition)
                .category(category)
                .fullName("Maria Lopez")
                .status(AthleteStatus.CONFIRMED)
                .checkedIn(false)
                .build());
    admin =
        accounts.saveAndFlush(
            UserAccount.builder()
                .email("admin@example.test")
                .role(UserRole.ADMIN)
                .passwordHash(encoder.encode("admin-test-password"))
                .enabled(true)
                .build());
    event =
        events.saveAndFlush(
            CompetitionEvent.builder()
                .competition(competition)
                .eventCode("1")
                .name("WOD 1")
                .scoreType(ScoreType.FOR_TIME)
                .rankingDirection(RankingDirection.LOWER_IS_BETTER)
                .cappedScoringEnabled(true)
                .totalReps(99)
                .timeCapSeconds(600)
                .tiebreakType(TiebreakType.NONE)
                .tiebreakRequired(false)
                .displayOrder(1)
                .publicVisible(true)
                .scoreVisible(true)
                .status(EventStatus.PUBLISHED)
                .build());
    heat =
        heats.saveAndFlush(
            CompetitionHeat.builder()
                .competition(competition)
                .event(event)
                .name("Heat 1")
                .heatNumber(1)
                .scheduledTime(LocalDateTime.now(competitionClock).plusMinutes(10))
                .status(HeatStatus.SCHEDULED)
                .capacity(10)
                .displayOrder(1)
                .publicVisible(true)
                .build());
    position =
        positions.saveAndFlush(
            CompetitionHeatAthlete.builder()
                .heat(heat)
                .athlete(athlete)
                .positionNumber(1)
                .checkInStatus(CheckInStatus.NOT_OPEN)
                .build());
    otherPosition =
        positions.saveAndFlush(
            CompetitionHeatAthlete.builder()
                .heat(heat)
                .athlete(other)
                .positionNumber(2)
                .checkInStatus(CheckInStatus.NOT_OPEN)
                .build());
  }

  Cookie token(UserAccount account) {
    return new Cookie("sivarfest_token", jwt.generateToken(new UserPrincipal(account)));
  }

  void activate() {
    access.provision(
        competition.getId(),
        new AthleteAccessRequest(
            "test-temp", List.of(new AthleteAccessRequest.Entry(athlete.getId(), "maria.lopez"))));
  }

  Cookie activeToken() {
    activate();
    var user = athlete.getUserAccount();
    user.setMustChangePassword(false);
    accounts.saveAndFlush(user);
    return token(user);
  }

  @Test
  void temporaryLoginRequiresChangeAndInvalidatesOldSession() throws Exception {
    activate();
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"MARIA.LOPEZ\",\"password\":\"test-temp\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mustChangePassword").value(true))
            .andReturn();
    Cookie temporary = login.getResponse().getCookie("sivarfest_token");
    mvc.perform(get("/api/auth/me").cookie(temporary)).andExpect(status().isOk());
    mvc.perform(get("/api/athlete/me").cookie(temporary))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    mvc.perform(
            put("/api/athlete/me/profile")
                .cookie(temporary)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/auth/change-password")
                .cookie(temporary)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"wrong\",\"newPassword\":\"my new secret phrase\"}"))
        .andExpect(status().isForbidden());
    var changed =
        mvc.perform(
                post("/api/auth/change-password")
                    .cookie(temporary)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"currentPassword\":\"test-temp\",\"newPassword\":\"my new secret"
                            + " phrase\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mustChangePassword").value(false))
            .andReturn();
    mvc.perform(get("/api/athlete/me").cookie(temporary)).andExpect(status().isForbidden());
    mvc.perform(get("/api/athlete/me").cookie(changed.getResponse().getCookie("sivarfest_token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profile.fullName").value("María López"));
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria.lopez\",\"password\":\"test-temp\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void provisioningIsIdempotentAndResetIsExplicit() throws Exception {
    var preview = access.preview(competition.getId());
    assertThat(preview).extracting("username").doesNotHaveDuplicates();
    var current = activeToken();
    String password = athlete.getUserAccount().getPasswordHash();
    access.provision(
        competition.getId(),
        new AthleteAccessRequest(
            "another-temp",
            List.of(new AthleteAccessRequest.Entry(athlete.getId(), "maria.lopez"))));
    assertThat(athlete.getUserAccount().getPasswordHash()).isEqualTo(password);
    assertThat(athlete.getUserAccount().isMustChangePassword()).isFalse();
    mvc.perform(
            post("/api/admin/athletes/" + athlete.getId() + "/reset-access")
                .cookie(token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        new AthleteAccessRequest(
                            "another-temp",
                            List.of(
                                new AthleteAccessRequest.Entry(athlete.getId(), "maria.lopez"))))))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/athlete/me").cookie(current)).andExpect(status().isForbidden());
    assertThat(athlete.getUserAccount().isMustChangePassword()).isTrue();
  }

  @Test
  void ownProfileAndPublicPrivacyAreEnforced() throws Exception {
    Cookie user = activeToken();
    mvc.perform(get("/api/public/competitions/experience-test/athletes/" + athlete.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.athlete.height").isEmpty())
        .andExpect(jsonPath("$.athlete.email").doesNotExist());
    mvc.perform(
            put("/api/athlete/me/profile")
                .cookie(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"country\":\"El Salvador\",\"gymName\":\"My"
                        + " Gym\",\"height\":165,\"weight\":60,\"showBodyMetrics\":true,\"fullName\":\"Tampered\",\"categoryId\":999}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("María López"));
    assertThat(other.getGymName()).isNull();
    mvc.perform(get("/api/public/competitions/experience-test/athletes/" + athlete.getId()))
        .andExpect(jsonPath("$.athlete.height").value(165));
    mvc.perform(
            get("/api/admin/competitions/" + competition.getId() + "/athlete-access").cookie(user))
        .andExpect(status().isForbidden());
    competition.setVisibilityStatus(VisibilityStatus.PRIVATE);
    competitions.saveAndFlush(competition);
    mvc.perform(get("/api/public/competitions/experience-test/athletes/" + athlete.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void checkInEnforcesOwnershipWindowAndIdempotency() throws Exception {
    Cookie user = activeToken();
    mvc.perform(post("/api/athlete/me/heats/" + otherPosition.getId() + "/check-in").cookie(user))
        .andExpect(status().isForbidden());
    heat.setScheduledTime(LocalDateTime.now(competitionClock).plusHours(2));
    heats.saveAndFlush(heat);
    mvc.perform(post("/api/athlete/me/heats/" + position.getId() + "/check-in").cookie(user))
        .andExpect(status().isConflict());
    heat.setScheduledTime(LocalDateTime.now(competitionClock).plusMinutes(10));
    heats.saveAndFlush(heat);
    mvc.perform(get("/api/athlete/me").cookie(user))
        .andExpect(jsonPath("$.heats[0].canCheckIn").value(true));
    mvc.perform(post("/api/athlete/me/heats/" + position.getId() + "/check-in").cookie(user))
        .andExpect(status().isNoContent());
    var checked = position.getCheckInTime();
    mvc.perform(post("/api/athlete/me/heats/" + position.getId() + "/check-in").cookie(user))
        .andExpect(status().isNoContent());
    assertThat(position.getCheckInTime()).isEqualTo(checked);
  }

  @Test
  void judgesCanOnlyConfirmAndScoreTheirOwnAssignments() throws Exception {
    var judgeUser =
        accounts.saveAndFlush(
            UserAccount.builder()
                .email("judge@test.local")
                .role(UserRole.JUDGE)
                .passwordHash("unused")
                .enabled(true)
                .build());
    var judge =
        judges.saveAndFlush(
            CompetitionJudge.builder()
                .competition(competition)
                .userAccount(judgeUser)
                .fullName("Judge Test")
                .email("judge@test.local")
                .active(true)
                .build());
    var assignment =
        assignments.saveAndFlush(
            CompetitionJudgeAssignment.builder()
                .judge(judge)
                .heat(heat)
                .heatAssignment(position)
                .build());
    Cookie cookie = token(judgeUser);
    mvc.perform(get("/api/judge/assignments").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].totalReps").value(99));
    mvc.perform(post("/api/judge/assignments/999999/check-in").cookie(cookie))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/judge/assignments/" + assignment.getId() + "/check-in").cookie(cookie))
        .andExpect(status().isNoContent());
    mvc.perform(
            put("/api/judge/assignments/" + assignment.getId() + "/score")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"scoreSeconds\":580}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
    mvc.perform(get("/api/public/competitions/experience-test/athletes/" + athlete.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.standing.eventResults[0].scoreId").isEmpty());
    mvc.perform(post("/api/admin/scores/1/publish").cookie(cookie))
        .andExpect(status().isForbidden());
  }

  @Test
  void photoUploadRejectsNonImagesAndRespectsPublicRemoval() throws Exception {
    Cookie user = activeToken();
    mvc.perform(
            multipart("/api/athlete/me/photo")
                .file(new MockMultipartFile("file", "x.svg", "image/svg+xml", "<svg/>".getBytes()))
                .cookie(user))
        .andExpect(status().isBadRequest());
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(
        new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB),
        "png",
        bytes);
    var result =
        mvc.perform(
                multipart("/api/athlete/me/photo")
                    .file(new MockMultipartFile("file", "x.png", "image/png", bytes.toByteArray()))
                    .cookie(user))
            .andExpect(status().isOk())
            .andReturn();
    String url = json.readTree(result.getResponse().getContentAsString()).get("url").asText();
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    mvc.perform(delete("/api/athlete/me/photo").cookie(user)).andExpect(status().isNoContent());
    mvc.perform(get(url)).andExpect(status().isNotFound());
  }

  @Test
  void announcementsKeepPrivateAudiencesPrivate() throws Exception {
    Cookie user = activeToken();
    for (String audience : List.of("PUBLIC", "ATHLETES", "JUDGES")) {
      mvc.perform(
              post("/api/admin/competitions/" + competition.getId() + "/announcements")
                  .cookie(token(admin))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"title\":\""
                          + audience
                          + "\",\"message\":\"Test message\",\"audience\":\""
                          + audience
                          + "\",\"published\":true}"))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/api/public/competitions/experience-test/announcements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].audience").value("PUBLIC"));
    mvc.perform(get("/api/athlete/me/announcements").cookie(user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(get("/api/judge/announcements").cookie(user)).andExpect(status().isForbidden());
  }

  @Test
  void nameBasedInitialPasswordUsesNormalizedFirstName() throws Exception {
    access.provision(
        competition.getId(),
        new AthleteAccessRequest(
            null, List.of(new AthleteAccessRequest.Entry(athlete.getId(), "maria.lopez")), true));
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria.lopez\",\"password\":\"mariasf!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mustChangePassword").value(true));
  }
}
