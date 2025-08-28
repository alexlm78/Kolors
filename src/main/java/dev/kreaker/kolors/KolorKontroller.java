// src/main/java/com/example/colorcombinations/ColorController.java
package dev.kreaker.kolors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class KolorKontroller {
   private final KolorKombinationRepository repo;

   public KolorKontroller(KolorKombinationRepository repo) {
      this.repo = repo;
   }

   @GetMapping("/")
   public String index(Model model) {
      // Redirect to new combinations interface
      return "redirect:/combinations/";
   }

   @PostMapping("/add")
   public String addColor(@ModelAttribute KolorKombination colorCombination) {
      // Validar que el hex tenga 6 caracteres y sea válido
      if (colorCombination.getHex() != null && colorCombination.getHex().matches("^[0-9A-Fa-f]{6}$")) {
         repo.save(colorCombination);
      }
      return "redirect:/";
   }
}
