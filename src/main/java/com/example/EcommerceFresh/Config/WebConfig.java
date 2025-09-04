package com.example.EcommerceFresh.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);
    
    @Value("${product.images.dir:/opt/ecommerce/productImages}")
    private String productImagesDir;

    @PostConstruct
    public void init() {
        // Create product images directory if it doesn't exist
        File directory = new File(productImagesDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                logger.info("Created product images directory: {}", productImagesDir);
            } else {
                logger.error("Failed to create product images directory: {}", productImagesDir);
                throw new RuntimeException("Cannot create product images directory: " + productImagesDir);
            }
        }
        
        // Validate directory is writable
        if (!directory.canWrite()) {
            logger.error("Product images directory is not writable: {}", productImagesDir);
            throw new RuntimeException("Product images directory is not writable: " + productImagesDir);
        }
        
        logger.info("Product images directory configured: {}", directory.getAbsolutePath());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded product images from persistent external folder
        String normalized = productImagesDir.endsWith("/") ? productImagesDir : productImagesDir + "/";
        String productImagesPath = "file:" + normalized;
        
        logger.info("Mapping /productImages/** to {}", productImagesPath);
        registry.addResourceHandler("/productImages/**")
                .addResourceLocations(productImagesPath)
                .setCachePeriod(31536000); 
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(31536000);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(31536000);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(31536000);


        // Note: Other static resources (/css/**, /js/**, /images/**, /static/**) 
        // are served by Spring Boot defaults from classpath:/static/
    }
}
