package com.bautruc.ecommerce.common.storage;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("local")
public class LocalObjectStorageConfiguration implements WebMvcConfigurer {
    private final Path root;

    public LocalObjectStorageConfiguration(
            @Value("${bautruc.local-storage.root:/var/lib/bautruc/objects}") Path root
    ) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Bean
    ObjectStoragePort objectStoragePort(
            @Value("${bautruc.local-storage.public-base-url:http://127.0.0.1:8080/local-media}") String publicBaseUrl
    ) {
        return new LocalObjectStorageAdapter(root, publicBaseUrl);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/local-media/**")
                .addResourceLocations(location);
    }
}
