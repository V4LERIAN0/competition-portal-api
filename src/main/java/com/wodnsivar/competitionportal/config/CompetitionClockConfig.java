package com.wodnsivar.competitionportal.config;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class CompetitionClockConfig {
  @Bean
  public Clock competitionClock(
      @Value("${app.competition-timezone:America/El_Salvador}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }
}
