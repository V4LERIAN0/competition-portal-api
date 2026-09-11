package com.wodnsivar.competitionportal.athlete.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AthleteProfileUpdateRequest(
    @Size(max = 80) String country,
    @Size(max = 150) String gymName,
    @DecimalMin("50") @DecimalMax("300") BigDecimal height,
    @DecimalMin("20") @DecimalMax("500") BigDecimal weight,
    @Size(max = 1500) String publicBio,
    boolean showBodyMetrics) {}
