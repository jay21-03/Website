package com.bautruc.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BautrucEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BautrucEcommerceApplication.class, args);
    }
}
