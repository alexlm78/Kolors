package dev.kreaker.kolors.security.service;

import dev.kreaker.kolors.security.model.KolorsUser;
import dev.kreaker.kolors.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KolorsUserDetailsService implements UserDetailsService {

    private static final Logger logger =
            LoggerFactory.getLogger(KolorsUserDetailsService.class);

    private final UserRepository userRepository;

    public KolorsUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);

        KolorsUser kolorsUser =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "User not found: " + username));

        return User.builder()
                .username(kolorsUser.getUsername())
                .password(kolorsUser.getPassword())
                .disabled(!kolorsUser.isEnabled())
                .roles("USER")
                .build();
    }
}
