/* (c) 2026 Alejandro Lopez Monzon <alejandro@kreaker.dev> for Kreaker Developments */
package dev.kreaker.kolors.security.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

   @Value("${jwt.secret.key}")
   private String jwtSecret;

   @Bean
   public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
   }

   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
               .csrf(csrf -> csrf.disable()).authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**",
                                 "/favicon.ico")
                        .permitAll().requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/combinations/search", "/api/combinations/{id}")
                        .permitAll().requestMatchers("/admin/migration-status").permitAll()
                        .requestMatchers("/mobile-test.html").permitAll()
                        // Swagger UI and API Docs
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()

                        // Protected endpoints (require authentication)
                        .requestMatchers("/api/combinations/**").authenticated()
                        .requestMatchers("/admin/**").authenticated()

                        // All other requests
                        .anyRequest().authenticated())
               .formLogin(form -> form.loginPage("/auth/login").loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/", true).failureUrl("/auth/login?error=true")
                        .permitAll())
               .logout(logout -> logout.logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout").invalidateHttpSession(true)
                        .clearAuthentication(true).deleteCookies("JSESSIONID"))
               .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).maximumSessions(1)
                        .maxSessionsPreventsLogin(false).expiredUrl("/auth/login?expired"));

      return http.build();
   }

   @Bean
   public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration configuration = new CorsConfiguration();
      configuration.setAllowedOriginPatterns(Arrays.asList("*"));
      configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
      configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
      configuration.setAllowCredentials(true);
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
   }
}
