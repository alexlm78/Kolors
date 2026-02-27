package dev.kreaker.kolors.security.controller;

import dev.kreaker.kolors.security.model.User;
import dev.kreaker.kolors.security.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository userRepository;

    public GlobalModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()) && auth.getName() != null) {
            Optional<User> userOpt = userRepository.findByUsername(auth.getName());
            return userOpt.orElse(null);
        }
        return null;
    }
}
