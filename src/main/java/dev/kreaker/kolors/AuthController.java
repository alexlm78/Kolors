package dev.kreaker.kolors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Show login form
     */
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "auth/login";
    }

    /**
     * Process login form (this will be handled by Spring Security)
     * This method is just for showing the form, actual authentication is handled by Spring Security
     */
    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginForm") LoginForm loginForm,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/login";
        }

        // Spring Security will handle the actual authentication
        // If login fails, Spring Security will redirect back to /login?error
        return "redirect:/";
    }

    /**
     * Show registration form
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    /**
     * Process registration form
     */
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerForm") RegisterForm registerForm,
                                     BindingResult result,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            // Check if username already exists
            if (userService.findByUsername(registerForm.getUsername()).isPresent()) {
                result.rejectValue("username", "error.registerForm", "Username already exists");
                return "auth/register";
            }

            // Check if email already exists
            if (userService.findByEmail(registerForm.getEmail()).isPresent()) {
                result.rejectValue("email", "error.registerForm", "Email already exists");
                return "auth/register";
            }

            // Validate password strength
            if (!isPasswordStrong(registerForm.getPassword())) {
                result.rejectValue("password", "error.registerForm",
                    "Password must be at least 8 characters long and contain uppercase, lowercase, number, and special character");
                return "auth/register";
            }

            // Check password confirmation
            if (!registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.registerForm", "Passwords do not match");
                return "auth/register";
            }

            // Register the user
            userService.registerUser(registerForm.getUsername(), registerForm.getEmail(), registerForm.getPassword());

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            result.reject("error.registerForm", "Registration failed: " + e.getMessage());
            return "auth/register";
        }
    }

    /**
     * Logout user
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/auth/login?logout";
    }

    /**
     * Check if password meets strength requirements
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Form classes for login and registration
     */
    public static class LoginForm {
        private String username;
        private String password;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterForm {
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        private String username;

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        private String password;

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        private String confirmPassword;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
