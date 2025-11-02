package dev.kreaker.kolors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user
     */
    public User registerUser(String username, String email, String rawPassword) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }

        // Create new user
        User user = new User(username, email, passwordEncoder.encode(rawPassword));

        // Assign default role
        user.addRole("USER");

        return userRepository.save(user);
    }

    /**
     * Authenticate user for login
     */
    public Optional<User> authenticateUser(String usernameOrEmail, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(rawPassword, user.getPassword()) && user.getEnabled()) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Update user password
     */
    public void updatePassword(Long userId, String newRawPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    /**
     * Enable/disable user account
     */
    public void setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /**
     * Add role to user
     */
    public void addRoleToUser(Long userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.addRole(role);
        userRepository.save(user);
    }

    /**
     * Remove role from user
     */
    public void removeRoleFromUser(Long userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.removeRole(role);
        userRepository.save(user);
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(Long userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return user.hasRole(role);
    }

    /**
     * Get user roles
     */
    public Set<String> getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return new HashSet<>(user.getRoles());
    }

    /**
     * Get total count of enabled users
     */
    @Transactional(readOnly = true)
    public long getEnabledUsersCount() {
        return userRepository.countEnabledUsers();
    }
}
