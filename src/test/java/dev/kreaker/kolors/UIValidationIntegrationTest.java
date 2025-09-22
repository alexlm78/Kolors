package dev.kreaker.kolors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.kreaker.kolors.service.ColorCombinationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * UI Validation Integration Tests Tests user interface behavior, responsiveness, and cross-browser
 * compatibility
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("UI Validation Integration Tests")
class UIValidationIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private ColorCombinationService colorCombinationService;

  @Autowired private ColorCombinationRepository colorCombinationRepository;

  @Autowired private ColorInCombinationRepository colorInCombinationRepository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    // Clean database before each test
    colorInCombinationRepository.deleteAll();
    colorCombinationRepository.deleteAll();
  }

  @Nested
  @DisplayName("Page Structure and Content Validation")
  class PageStructureValidationTests {

    @Test
    @DisplayName("Should render main page with proper HTML structure")
    void shouldRenderMainPageWithProperHtmlStructure() throws Exception {
      // Create test data
      createTestCombination("Test Combination", "FF0000", "00FF00");

      MvcResult result =
          mockMvc
              .perform(get("/combinations/"))
              .andExpect(status().isOk())
              .andExpect(view().name("combinations/index"))
              .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
              .andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate essential HTML structure
      assertThat(content).contains("<!DOCTYPE html>");
      assertThat(content).contains("<html");
      assertThat(content).contains("<head>");
      assertThat(content).contains("<body>");
      assertThat(content).contains("</html>");

      // Validate meta tags for responsive design
      assertThat(content).contains("viewport");
      assertThat(content).contains("width=device-width");

      // Validate form structure
      assertThat(content).contains("<form");
      assertThat(content).contains("method=\"post\"");
      assertThat(content).contains("action=\"/combinations/create\"");

      // Validate color input fields
      assertThat(content).contains("type=\"text\"");
      assertThat(content).contains("name=\"colors[0].hexValue\"");
      assertThat(content).contains("pattern=\"[0-9A-Fa-f]{6}\"");

      // Validate JavaScript inclusion
      assertThat(content).contains("<script");
      assertThat(content).contains("mobile-enhancements.js");
    }

    @Test
    @DisplayName("Should render edit page with proper form structure")
    void shouldRenderEditPageWithProperFormStructure() throws Exception {
      // Create test combination
      ColorCombination combination =
          createTestCombination("Edit Test", "FF0000", "00FF00", "0000FF");

      MvcResult result =
          mockMvc
              .perform(get("/combinations/" + combination.getId() + "/edit"))
              .andExpect(status().isOk())
              .andExpect(view().name("combinations/edit"))
              .andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate edit form structure
      assertThat(content).contains("action=\"/combinations/" + combination.getId() + "/update\"");
      assertThat(content).contains("value=\"Edit Test\"");
      assertThat(content).contains("value=\"FF0000\"");
      assertThat(content).contains("value=\"00FF00\"");
      assertThat(content).contains("value=\"0000FF\"");

      // Validate dynamic color management buttons
      assertThat(content).contains("Add Color");
      assertThat(content).contains("Remove");
      assertThat(content).contains("btn-add-color");
      assertThat(content).contains("btn-remove-color");

      // Validate color preview elements
      assertThat(content).contains("color-preview");
      assertThat(content).contains("background-color: #FF0000");
      assertThat(content).contains("background-color: #00FF00");
      assertThat(content).contains("background-color: #0000FF");
    }

    @Test
    @DisplayName("Should render view page with proper color display")
    void shouldRenderViewPageWithProperColorDisplay() throws Exception {
      // Create test combination
      ColorCombination combination =
          createTestCombination("View Test", "FF6B35", "F7931E", "FFD23F");

      MvcResult result =
          mockMvc
              .perform(get("/combinations/" + combination.getId()))
              .andExpect(status().isOk())
              .andExpect(view().name("combinations/view"))
              .andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate color display
      assertThat(content).contains("View Test");
      assertThat(content).contains("#FF6B35");
      assertThat(content).contains("#F7931E");
      assertThat(content).contains("#FFD23F");

      // Validate color swatches
      assertThat(content).contains("color-swatch");
      assertThat(content).contains("background-color: #FF6B35");
      assertThat(content).contains("background-color: #F7931E");
      assertThat(content).contains("background-color: #FFD23F");

      // Validate action buttons
      assertThat(content).contains("Edit");
      assertThat(content).contains("Delete");
      assertThat(content).contains("href=\"/combinations/" + combination.getId() + "/edit\"");
    }
  }

  @Nested
  @DisplayName("Responsive Design Validation")
  class ResponsiveDesignValidationTests {

    @Test
    @DisplayName("Should include responsive CSS and meta tags")
    void shouldIncludeResponsiveCssAndMetaTags() throws Exception {
      MvcResult result =
          mockMvc.perform(get("/combinations/")).andExpect(status().isOk()).andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate viewport meta tag
      assertThat(content).contains("name=\"viewport\"");
      assertThat(content).contains("content=\"width=device-width, initial-scale=1.0\"");

      // Validate responsive CSS inclusion
      assertThat(content).contains("mobile-responsive.css");

      // Validate CSS classes for responsive design
      assertThat(content).contains("container");
      assertThat(content).contains("row");
      assertThat(content).contains("col-");

      // Validate mobile-friendly form elements
      assertThat(content).contains("form-control");
      assertThat(content).contains("btn");
    }

    @Test
    @DisplayName("Should render mobile test page correctly")
    void shouldRenderMobileTestPageCorrectly() throws Exception {
      MvcResult result =
          mockMvc.perform(get("/mobile-test.html")).andExpect(status().isOk()).andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate mobile test page structure
      assertThat(content).contains("Mobile Test Page");
      assertThat(content).contains("viewport");
      assertThat(content).contains("mobile-responsive.css");
      assertThat(content).contains("mobile-enhancements.js");

      // Validate test elements for different screen sizes
      assertThat(content).contains("test-desktop");
      assertThat(content).contains("test-tablet");
      assertThat(content).contains("test-mobile");
    }

    @Test
    @DisplayName("Should handle different screen size simulations")
    void shouldHandleDifferentScreenSizeSimulations() throws Exception {
      // Create test data with various color counts
      createTestCombination("Single Color", "FF0000");
      createTestCombination("Two Colors", "FF0000", "00FF00");
      createTestCombination("Three Colors", "FF0000", "00FF00", "0000FF");
      createTestCombination("Four Colors", "FF0000", "00FF00", "0000FF", "FFFF00");

      // Test with different user agents (simulating different devices)
      String[] userAgents = {
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)", // Mobile
        "Mozilla/5.0 (iPad; CPU OS 14_0 like Mac OS X)", // Tablet
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" // Desktop
      };

      for (String userAgent : userAgents) {
        MvcResult result =
            mockMvc
                .perform(get("/combinations/").header("User-Agent", userAgent))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        // Validate that content is properly structured for all devices
        assertThat(content).contains("mobile-responsive.css");
        assertThat(content).contains("viewport");
        assertThat(content).contains("Single Color");
        assertThat(content).contains("Four Colors");
      }
    }
  }

  @Nested
  @DisplayName("Form Validation and Interaction")
  class FormValidationTests {

    @Test
    @DisplayName("Should include proper form validation attributes")
    void shouldIncludeProperFormValidationAttributes() throws Exception {
      MvcResult result =
          mockMvc.perform(get("/combinations/")).andExpect(status().isOk()).andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate HTML5 form validation attributes
      assertThat(content).contains("required");
      assertThat(content).contains("pattern=\"[0-9A-Fa-f]{6}\"");
      assertThat(content).contains("minlength=\"3\"");
      assertThat(content).contains("maxlength=\"100\"");

      // Validate input types
      assertThat(content).contains("type=\"text\"");
      assertThat(content).contains("type=\"submit\"");

      // Validate CSRF protection
      assertThat(content).contains("_csrf");
      assertThat(content).contains("name=\"_csrf\"");
    }

    @Test
    @DisplayName("Should display validation errors properly")
    void shouldDisplayValidationErrorsProperly() throws Exception {
      // Submit invalid form data
      MvcResult result =
          mockMvc
              .perform(
                  post("/combinations/create")
                      .param("name", "ab") // Too short
                      .param("colorCount", "1")
                      .param("colors[0].hexValue", "INVALID") // Invalid hex
                      .param("colors[0].position", "1"))
              .andExpect(status().isOk())
              .andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate error display
      assertThat(content).contains("error");
      assertThat(content).contains("alert");

      // Validate form retains user input
      assertThat(content).contains("value=\"ab\"");
      assertThat(content).contains("value=\"INVALID\"");
    }

    @Test
    @DisplayName("Should include JavaScript for dynamic form behavior")
    void shouldIncludeJavaScriptForDynamicFormBehavior() throws Exception {
      ColorCombination combination = createTestCombination("JS Test", "FF0000");

      MvcResult result =
          mockMvc
              .perform(get("/combinations/" + combination.getId() + "/edit"))
              .andExpect(status().isOk())
              .andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate JavaScript inclusion
      assertThat(content).contains("mobile-enhancements.js");
      assertThat(content).contains("<script");

      // Validate dynamic elements for JavaScript interaction
      assertThat(content).contains("btn-add-color");
      assertThat(content).contains("btn-remove-color");
      assertThat(content).contains("color-inputs-container");
      assertThat(content).contains("data-");
    }
  }

  @Nested
  @DisplayName("Accessibility Validation")
  class AccessibilityValidationTests {

    @Test
    @DisplayName("Should include proper accessibility attributes")
    void shouldIncludeProperAccessibilityAttributes() throws Exception {
      MvcResult result =
          mockMvc.perform(get("/combinations/")).andExpect(status().isOk()).andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate semantic HTML elements
      assertThat(content).contains("<main");
      assertThat(content).contains("<nav");
      assertThat(content).contains("<section");
      assertThat(content).contains("<article");

      // Validate form labels
      assertThat(content).contains("<label");
      assertThat(content).contains("for=");

      // Validate ARIA attributes
      assertThat(content).contains("aria-");
      assertThat(content).contains("role=");

      // Validate alt attributes for any images
      if (content.contains("<img")) {
        assertThat(content).contains("alt=");
      }
    }

    @Test
    @DisplayName("Should provide keyboard navigation support")
    void shouldProvideKeyboardNavigationSupport() throws Exception {
      ColorCombination combination = createTestCombination("Keyboard Test", "FF0000", "00FF00");

      MvcResult result =
          mockMvc
              .perform(get("/combinations/" + combination.getId() + "/edit"))
              .andExpect(status().isOk())
              .andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate tabindex attributes where needed
      assertThat(content).contains("tabindex");

      // Validate focusable elements
      assertThat(content).contains("type=\"text\"");
      assertThat(content).contains("type=\"submit\"");
      assertThat(content).contains("type=\"button\"");

      // Validate keyboard event handlers in JavaScript
      assertThat(content).contains("mobile-enhancements.js");
    }
  }

  @Nested
  @DisplayName("Cross-Browser Compatibility")
  class CrossBrowserCompatibilityTests {

    @Test
    @DisplayName("Should work with different browser user agents")
    void shouldWorkWithDifferentBrowserUserAgents() throws Exception {
      // Create test data
      createTestCombination("Browser Test", "FF0000", "00FF00");

      String[] browserUserAgents = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59"
      };

      for (String userAgent : browserUserAgents) {
        MvcResult result =
            mockMvc
                .perform(get("/combinations/").header("User-Agent", userAgent))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        String content = result.getResponse().getContentAsString();

        // Validate essential content is present for all browsers
        assertThat(content).contains("Browser Test");
        assertThat(content).contains("FF0000");
        assertThat(content).contains("00FF00");
        assertThat(content).contains("mobile-responsive.css");
        assertThat(content).contains("mobile-enhancements.js");
      }
    }

    @Test
    @DisplayName("Should handle different HTTP accept headers")
    void shouldHandleDifferentHttpAcceptHeaders() throws Exception {
      createTestCombination("Accept Test", "FF0000");

      String[] acceptHeaders = {
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
      };

      for (String acceptHeader : acceptHeaders) {
        mockMvc
            .perform(get("/combinations/").header("Accept", acceptHeader))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
      }
    }
  }

  @Nested
  @DisplayName("Dynamic Content Validation")
  class DynamicContentValidationTests {

    @Test
    @DisplayName("Should render combinations with different color counts correctly")
    void shouldRenderCombinationsWithDifferentColorCountsCorrectly() throws Exception {
      // Create combinations with different color counts
      createTestCombination("One Color", "FF0000");
      createTestCombination("Two Colors", "FF0000", "00FF00");
      createTestCombination("Three Colors", "FF0000", "00FF00", "0000FF");
      createTestCombination("Five Colors", "FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF");

      MvcResult result =
          mockMvc.perform(get("/combinations/")).andExpect(status().isOk()).andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate all combinations are displayed
      assertThat(content).contains("One Color");
      assertThat(content).contains("Two Colors");
      assertThat(content).contains("Three Colors");
      assertThat(content).contains("Five Colors");

      // Validate color counts are displayed
      assertThat(content).contains("1 color");
      assertThat(content).contains("2 colors");
      assertThat(content).contains("3 colors");
      assertThat(content).contains("5 colors");

      // Validate all hex values are displayed
      assertThat(content).contains("#FF0000");
      assertThat(content).contains("#00FF00");
      assertThat(content).contains("#0000FF");
      assertThat(content).contains("#FFFF00");
      assertThat(content).contains("#FF00FF");
    }

    @Test
    @DisplayName("Should handle search results display correctly")
    void shouldHandleSearchResultsDisplayCorrectly() throws Exception {
      // Create test data
      createTestCombination("Ocean Blue", "0077BE", "87CEEB");
      createTestCombination("Forest Green", "228B22", "32CD32");
      createTestCombination("Ocean Deep", "006994", "0077BE");

      // Test search by name
      MvcResult nameResult =
          mockMvc
              .perform(get("/combinations/").param("search", "Ocean"))
              .andExpect(status().isOk())
              .andReturn();

      String nameContent = nameResult.getResponse().getContentAsString();
      assertThat(nameContent).contains("Ocean Blue");
      assertThat(nameContent).contains("Ocean Deep");
      assertThat(nameContent).doesNotContain("Forest Green");

      // Test search by color count
      MvcResult countResult =
          mockMvc
              .perform(get("/combinations/").param("colorCount", "2"))
              .andExpect(status().isOk())
              .andReturn();

      String countContent = countResult.getResponse().getContentAsString();
      assertThat(countContent).contains("Ocean Blue");
      assertThat(countContent).contains("Forest Green");
      assertThat(countContent).contains("Ocean Deep");

      // Test search by hex value
      MvcResult hexResult =
          mockMvc
              .perform(get("/combinations/").param("hexValue", "0077BE"))
              .andExpect(status().isOk())
              .andReturn();

      String hexContent = hexResult.getResponse().getContentAsString();
      assertThat(hexContent).contains("Ocean Blue");
      assertThat(hexContent).contains("Ocean Deep");
      assertThat(hexContent).doesNotContain("Forest Green");
    }

    @Test
    @DisplayName("Should display empty state correctly")
    void shouldDisplayEmptyStateCorrectly() throws Exception {
      // Test with no combinations
      MvcResult result =
          mockMvc.perform(get("/combinations/")).andExpect(status().isOk()).andReturn();

      String content = result.getResponse().getContentAsString();

      // Validate empty state message
      assertThat(content)
          .containsAnyOf(
              "No combinations found", "No color combinations", "Create your first", "empty");

      // Validate form is still present
      assertThat(content).contains("<form");
      assertThat(content).contains("Create");
    }
  }

  private ColorCombination createTestCombination(String name, String... hexValues) {
    ColorCombinationForm form = new ColorCombinationForm(name);
    for (String hexValue : hexValues) {
      form.addColor(hexValue);
    }
    return colorCombinationService.createCombination(form);
  }
}
