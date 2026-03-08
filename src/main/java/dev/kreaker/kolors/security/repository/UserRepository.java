/* (c) 2026 Alejandro Lopez Monzon <alejandro@kreaker.dev> for Kreaker Developments */
package dev.kreaker.kolors.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.kreaker.kolors.security.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

   Optional<User> findByUsername(String username);

   Optional<User> findByEmail(String email);

   boolean existsByUsername(String username);

   boolean existsByEmail(String email);

   @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
   Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

   @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
   long countEnabledUsers();
}
