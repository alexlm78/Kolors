package dev.kreaker.kolors.security.repository;

import dev.kreaker.kolors.security.model.KolorsUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<KolorsUser, Long> {

    Optional<KolorsUser> findByUsername(String username);

    Optional<KolorsUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
