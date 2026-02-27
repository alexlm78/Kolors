package dev.kreaker.kolors.security.config;

import dev.kreaker.kolors.security.model.User;
import dev.kreaker.kolors.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("kreaker")) {
            User seedUser =
                    new User(
                            "kreaker",
                            "alejandro@kreaker.dev",
                            passwordEncoder.encode("kreaker123"),
                            "Kreaker");
            
            seedUser.addRole("ADMIN");
            seedUser.addRole("USER");

            userRepository.save(seedUser);
            logger.info("Seed user 'kreaker' created successfully");
        } else {
            logger.debug("Seed user 'kreaker' already exists, skipping creation");
        }
    }
}
