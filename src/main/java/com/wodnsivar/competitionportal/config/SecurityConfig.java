package com.wodnsivar.competitionportal.config;
import com.wodnsivar.competitionportal.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration @RequiredArgsConstructor
public class SecurityConfig {
 private final JwtAuthenticationFilter jwtAuthenticationFilter;
 private final UserDetailsService userDetailsService;
 @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{
  http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable)
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a
    .requestMatchers("/api/auth/login","/api/auth/logout","/api/auth/me","/api/public/**").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/athlete/**").hasRole("ATHLETE")
    .requestMatchers("/api/judge/**").hasRole("JUDGE")
    .anyRequest().authenticated())
   .authenticationProvider(authenticationProvider())
   .addFilterBefore(jwtAuthenticationFilter,UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
 @Bean public AuthenticationProvider authenticationProvider(){
  DaoAuthenticationProvider p=new DaoAuthenticationProvider(userDetailsService);p.setPasswordEncoder(passwordEncoder());return p;
 }
 @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration c)throws Exception{return c.getAuthenticationManager();}
 @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
}
