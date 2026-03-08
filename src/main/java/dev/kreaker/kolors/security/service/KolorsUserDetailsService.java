/* (c) 2026 Alejandro Lopez Monzon <alejandro@kreaker.dev> for Kreaker Developments */
package dev.kreaker.kolors.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.kreaker.kolors.security.model.User;
import dev.kreaker.kolors.security.repository.UserRepository;

@Service
public class KolorsUserDetailsService implements UserDetailsService {

   private static final Logger logger = LoggerFactory.getLogger(KolorsUserDetailsService.class);

   private final UserRepository userRepository;

   public KolorsUserDetailsService(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @Override
   @Transactional(readOnly = true)
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      logger.debug("Loading user by username: {}", username);

      User user = userRepository.findByUsername(username)
               .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

      return org.springframework.security.core.userdetails.User.builder()
               .username(user.getUsername()).password(user.getPassword())
               .disabled(!user.getEnabled())
               .authorities(
                        user.getRoles().stream().map(role -> "ROLE_" + role).toArray(String[]::new))
               .build();
   }
}
