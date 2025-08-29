package com.example.EcommerceFresh.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded product images from an external folder at <projectRoot>/productImages
        String productImagesPath = "file:" + System.getProperty("user.dir") + "/productImages/";
        registry.addResourceHandler("/productImages/**")
                .addResourceLocations(productImagesPath);

        // Ensure existing static images still served from classpath
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
