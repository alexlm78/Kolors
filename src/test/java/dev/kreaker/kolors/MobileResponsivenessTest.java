package dev.kreaker.kolors;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Test class to verify mobile responsiveness implementation */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class MobileResponsivenessTest {

  @Test
  public void shouldHaveMobileResponsiveCssFile() throws IOException {
    Path cssPath = Paths.get("src/main/resources/static/css/mobile-responsive.css");
    assertTrue(Files.exists(cssPath), "Mobile responsive CSS file should exist");

    String cssContent = Files.readString(cssPath);

    // Verify key mobile-first responsive features
    assertTrue(cssContent.contains("@media (min-width: 768px)"), "Should have tablet breakpoint");
    assertTrue(cssContent.contains("@media (min-width: 1024px)"), "Should have desktop breakpoint");
    assertTrue(cssContent.contains("touch-action: manipulation"), "Should have touch optimization");
    assertTrue(cssContent.contains("min-height: 48px"), "Should have minimum touch target size");
    assertTrue(cssContent.contains("font-size: 16px"), "Should prevent iOS zoom with 16px font");
    assertTrue(
        cssContent.contains("-webkit-tap-highlight-color"),
        "Should have webkit touch optimizations");
  }

  @Test
  public void shouldHaveMobileEnhancementJavaScript() throws IOException {
    Path jsPath = Paths.get("src/main/resources/static/js/mobile-enhancements.js");
    assertTrue(Files.exists(jsPath), "Mobile enhancement JavaScript file should exist");

    String jsContent = Files.readString(jsPath);

    // Verify key mobile enhancement features
    assertTrue(
        jsContent.contains("initTouchFeedback"), "Should have touch feedback initialization");
    assertTrue(jsContent.contains("initMobileFormOptimizations"), "Should have form optimizations");
    assertTrue(jsContent.contains("initOrientationHandling"), "Should handle orientation changes");
    assertTrue(jsContent.contains("navigator.vibrate"), "Should have haptic feedback support");
    assertTrue(jsContent.contains("scrollIntoView"), "Should have scroll into view for mobile");
  }

  @Test
  public void shouldHaveResponsiveMetaTagInTemplates() throws IOException {
    String[] templatePaths = {
      "src/main/resources/templates/combinations/index.html",
      "src/main/resources/templates/combinations/edit.html",
      "src/main/resources/templates/combinations/view.html",
      "src/main/resources/templates/combinations/confirm-delete.html"
    };

    for (String templatePath : templatePaths) {
      Path path = Paths.get(templatePath);
      assertTrue(Files.exists(path), "Template should exist: " + templatePath);

      String content = Files.readString(path);
      assertTrue(
          content.contains("width=device-width, initial-scale=1.0"),
          "Template should have responsive viewport meta tag: " + templatePath);
      assertTrue(
          content.contains("/css/mobile-responsive.css"),
          "Template should include mobile responsive CSS: " + templatePath);
      assertTrue(
          content.contains("/js/mobile-enhancements.js"),
          "Template should include mobile enhancement JS: " + templatePath);
    }
  }

  @Test
  public void shouldHaveMobileOptimizedColorFields() throws IOException {
    Path cssPath = Paths.get("src/main/resources/static/css/mobile-responsive.css");
    String cssContent = Files.readString(cssPath);

    // Verify color field mobile optimizations
    assertTrue(
        cssContent.contains("flex-direction: column"), "Color fields should stack on mobile");
    assertTrue(
        cssContent.contains("text-align: center"), "Color fields should be centered on mobile");
    assertTrue(cssContent.contains("width: 60px"), "Color previews should be appropriately sized");
    assertTrue(cssContent.contains("height: 60px"), "Color previews should be appropriately sized");
  }

  @Test
  public void shouldHaveTouchOptimizedButtons() throws IOException {
    Path cssPath = Paths.get("src/main/resources/static/css/mobile-responsive.css");
    String cssContent = Files.readString(cssPath);

    // Verify button touch optimizations
    assertTrue(
        cssContent.contains("min-height: 48px"), "Buttons should meet minimum touch target size");
    assertTrue(
        cssContent.contains("min-width: 48px"), "Buttons should meet minimum touch target size");
    assertTrue(
        cssContent.contains("touch-action: manipulation"),
        "Buttons should have touch optimization");
    assertTrue(
        cssContent.contains("-webkit-tap-highlight-color: transparent"),
        "Should disable default tap highlight");
  }

  @Test
  public void shouldHaveResponsiveGridLayout() throws IOException {
    Path cssPath = Paths.get("src/main/resources/static/css/mobile-responsive.css");
    String cssContent = Files.readString(cssPath);

    // Verify responsive grid
    assertTrue(
        cssContent.contains("grid-template-columns: 1fr"), "Should have single column on mobile");
    assertTrue(
        cssContent.contains("grid-template-columns: repeat(auto-fill, minmax(300px, 1fr))"),
        "Should have responsive grid for larger screens");
  }

  @Test
  public void shouldHaveMobileFormOptimizations() throws IOException {
    Path cssPath = Paths.get("src/main/resources/static/css/mobile-responsive.css");
    String cssContent = Files.readString(cssPath);

    // Verify form optimizations
    assertTrue(
        cssContent.contains("padding: 14px 12px"), "Form controls should have adequate padding");
    assertTrue(cssContent.contains("font-size: 16px"), "Should prevent zoom on iOS");
    assertTrue(cssContent.contains("-webkit-appearance: none"), "Should normalize appearance");
    assertTrue(cssContent.contains("appearance: none"), "Should normalize appearance");
  }

  @Test
  public void shouldHaveAccessibilityFeatures() throws IOException {
    Path jsPath = Paths.get("src/main/resources/static/js/mobile-enhancements.js");
    String jsContent = Files.readString(jsPath);

    // Verify accessibility features
    assertTrue(
        jsContent.contains("initAccessibilityEnhancements"),
        "Should have accessibility enhancements");
    assertTrue(jsContent.contains("skip-link"), "Should have skip links");
    assertTrue(jsContent.contains("scrollIntoView"), "Should help with focus management");
    assertTrue(jsContent.contains("main-content"), "Should have main content identification");
  }

  @Test
  public void shouldHaveOrientationHandling() throws IOException {
    Path jsPath = Paths.get("src/main/resources/static/js/mobile-enhancements.js");
    String jsContent = Files.readString(jsPath);

    // Verify orientation handling
    assertTrue(jsContent.contains("orientationchange"), "Should handle orientation changes");
    assertTrue(jsContent.contains("--vh"), "Should handle viewport height issues");
    assertTrue(jsContent.contains("window.innerHeight"), "Should calculate proper viewport height");
  }

  @Test
  public void shouldHaveMobileTestPage() throws IOException {
    Path testPath = Paths.get("src/main/resources/static/mobile-test.html");
    assertTrue(Files.exists(testPath), "Mobile test page should exist");

    String content = Files.readString(testPath);
    assertTrue(content.contains("Mobile Responsiveness Test"), "Should be a mobile test page");
    assertTrue(content.contains("Touch-Friendly Form Controls"), "Should test form controls");
    assertTrue(content.contains("Dynamic Color Fields"), "Should test color fields");
    assertTrue(content.contains("updateScreenInfo"), "Should have screen size detection");
  }
}
