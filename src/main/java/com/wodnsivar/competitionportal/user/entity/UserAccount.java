package com.wodnsivar.competitionportal.user.entity;

import com.wodnsivar.competitionportal.common.audit.BaseEntity;
import com.wodnsivar.competitionportal.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "user_accounts",
    indexes = {@Index(name = "idx_user_accounts_email", columnList = "email")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 150)
  private String email;

  @Column(unique = true, length = 100)
  private String username;

  @Builder.Default
  @Column(
      name = "must_change_password",
      nullable = false,
      columnDefinition = "boolean default false")
  private boolean mustChangePassword = false;

  @Builder.Default
  @Column(name = "token_version", nullable = false, columnDefinition = "integer default 0")
  private int tokenVersion = 0;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserRole role;

  @Column(nullable = false)
  @Builder.Default
  private Boolean enabled = true;
}
