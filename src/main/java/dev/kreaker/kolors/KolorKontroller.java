// src/main/java/com/example/colorcombinations/ColorController.java
package dev.kreaker.kolors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class KolorKontroller {
   private final KolorKombinationRepository repo;

   public KolorKontroller(KolorKombinationRepository repo) {
      this.repo = repo;
   }

   @GetMapping("/")
   public String index(Model model) {
      model.addAttribute("colors", repo.findAll());
      model.addAttribute("colorCombination", new KolorKombination());
      return "index";
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
