/* (c) 2026 Alejandro Lopez Monzon <alejandro@kreaker.dev> for Kreaker Developments */
package dev.kreaker.kolors.security.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.kreaker.kolors.security.model.PasswordResetToken;
import dev.kreaker.kolors.security.model.User;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

   Optional<PasswordResetToken> findByToken(String token);

   Optional<PasswordResetToken> findByUserAndUsedFalse(User user);

   @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user = :user AND prt.used = false AND prt.expiryDate > :now")
   Optional<PasswordResetToken> findValidTokenByUser(@Param("user") User user,
            @Param("now") LocalDateTime now);

   @Modifying
   @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiryDate < :now")
   int deleteExpiredTokens(@Param("now") LocalDateTime now);

   @Modifying
   @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user = :user")
   int invalidateAllUserTokens(@Param("user") User user);
}
