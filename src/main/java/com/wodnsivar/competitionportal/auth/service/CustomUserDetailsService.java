package com.wodnsivar.competitionportal.auth.service;

import com.wodnsivar.competitionportal.auth.security.UserPrincipal;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import com.wodnsivar.competitionportal.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    String identifier = email.trim().toLowerCase(java.util.Locale.ROOT);
    UserAccount userAccount =
        (identifier.contains("@")
                ? userAccountRepository.findByEmail(identifier)
                : userAccountRepository.findByUsernameIgnoreCase(identifier))
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas."));

    return new UserPrincipal(userAccount);
  }
}
