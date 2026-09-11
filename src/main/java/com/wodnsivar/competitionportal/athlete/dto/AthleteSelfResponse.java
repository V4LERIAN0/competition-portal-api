package com.wodnsivar.competitionportal.athlete.dto;

import java.math.BigDecimal;

public record AthleteSelfResponse(
    Long id,
    String fullName,
    String username,
    String categoryName,
    String country,
    String gymName,
    BigDecimal height,
    BigDecimal weight,
    String publicBio,
    String profilePhotoUrl,
    boolean showBodyMetrics) {}
