// src/main/java/com/example/colorcombinations/ColorCombination.java
package dev.kreaker.kolors;

import jakarta.persistence.*;

@Entity
public class KolorKombination {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   private String name;

   @Column(length = 6)
   private String hex;

   public KolorKombination() {
   }

   public KolorKombination(String name, String hex) {
      this.name = name;
      this.hex = hex;
   }

   public Long getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public String getHex() {
      return hex;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setHex(String hex) {
      this.hex = hex;
   }
}
