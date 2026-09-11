package com.wodnsivar.competitionportal.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Usuario o correo requerido")
        @jakarta.validation.constraints.Size(max = 150)
        @com.fasterxml.jackson.annotation.JsonAlias("username")
        String email,
    @NotBlank(message = "Password is required") @jakarta.validation.constraints.Size(max = 72)
        String password) {}
