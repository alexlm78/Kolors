package dev.kreaker.kolors.security.controller;

import dev.kreaker.kolors.security.dto.RegisterDTO;
import dev.kreaker.kolors.security.model.KolorsUser;
import dev.kreaker.kolors.security.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        if (!registerDTO.isPasswordMatch()) {
            result.rejectValue("confirmPassword", "error.confirmPassword",
                    "Las contrasenas no coinciden");
            return "auth/register";
        }

        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            result.rejectValue("username", "error.username",
                    "El nombre de usuario ya existe");
            return "auth/register";
        }

        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            result.rejectValue("email", "error.email",
                    "El email ya esta registrado");
            return "auth/register";
        }

        KolorsUser newUser =
                new KolorsUser(
                        registerDTO.getUsername(),
                        registerDTO.getEmail(),
                        passwordEncoder.encode(registerDTO.getPassword()),
                        registerDTO.getDisplayName() != null
                                        && !registerDTO.getDisplayName().isBlank()
                                ? registerDTO.getDisplayName()
                                : registerDTO.getUsername());

        userRepository.save(newUser);
        logger.info("New user registered: {}", newUser.getUsername());

        redirectAttributes.addFlashAttribute("success",
                "Usuario '" + newUser.getUsername() + "' registrado exitosamente");
        return "redirect:/combinations/";
    }
}
