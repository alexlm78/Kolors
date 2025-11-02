package dev.kreaker.kolors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Token expiration time in hours
    private static final int TOKEN_EXPIRATION_HOURS = 24;

    @Autowired
    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                               UserService userService,
                               EmailService emailService,
                               PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Initiate password reset process for a user
     */
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Invalidate any existing tokens for this user
            tokenRepository.invalidateAllUserTokens(user);

            // Generate new token
            String token = generateToken();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
            tokenRepository.save(resetToken);

            // Send reset email
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        }
        // Don't reveal if email exists or not for security reasons
    }

    /**
     * Validate password reset token
     */
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isPresent()) {
            PasswordResetToken resetToken = tokenOpt.get();
            return !resetToken.isUsed() && !resetToken.isExpired();
        }

        return false;
    }

    /**
     * Reset password using valid token
     */
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isPresent()) {
            PasswordResetToken resetToken = tokenOpt.get();

            if (!resetToken.isUsed() && !resetToken.isExpired()) {
                // Update user password
                userService.updatePassword(resetToken.getUser().getId(), newPassword);

                // Mark token as used
                resetToken.setUsed(true);
                tokenRepository.save(resetToken);

                return true;
            }
        }

        return false;
    }

    /**
     * Get user associated with token (for display purposes)
     */
    public Optional<User> getUserByToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isPresent()) {
            PasswordResetToken resetToken = tokenOpt.get();
            if (!resetToken.isUsed() && !resetToken.isExpired()) {
                return Optional.of(resetToken.getUser());
            }
        }

        return Optional.empty();
    }

    /**
     * Generate secure random token
     */
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Scheduled task to clean up expired tokens (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = tokenRepository.deleteExpiredTokens(now);

        if (deletedCount > 0) {
            System.out.println("Cleaned up " + deletedCount + " expired password reset tokens");
        }
    }
}
