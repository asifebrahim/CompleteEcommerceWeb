package com.example.EcommerceFresh.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${product.images.dir:${user.dir}/productImages}")
    private String productImagesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded product images from configurable external folder
        String normalized = productImagesDir.endsWith("/") ? productImagesDir : productImagesDir + "/";
        String productImagesPath = "file:" + normalized;
        registry.addResourceHandler("/productImages/**")
                .addResourceLocations(productImagesPath);

        // Ensure existing static images still served from classpath
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
