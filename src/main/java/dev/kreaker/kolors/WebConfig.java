package dev.kreaker.kolors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for Kolors application. Configures static resource handling and view
 * controllers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure static resource handlers. Sets low order to ensure controllers have priority over
     * static resources.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure static resources are served with lower priority than controllers
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);

        // Set explicit order to ensure controllers are checked first
        registry.setOrder(Integer.MAX_VALUE);
    }

    /**
     * Configure simple automated controllers pre-configured with the response status and/or a view.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root to combinations
        registry.addRedirectViewController("/", "/combinations/");
    }
}
