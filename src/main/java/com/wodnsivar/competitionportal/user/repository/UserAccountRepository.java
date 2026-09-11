package com.wodnsivar.competitionportal.user.repository;

import com.wodnsivar.competitionportal.user.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<UserAccount> findByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from UserAccount u where u.id = :id")
  Optional<UserAccount> findForUpdate(
      @org.springframework.data.repository.query.Param("id") Long id);
}
