package com.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        // CRITICAL: Set timezone BEFORE Spring Boot starts
        // This must happen before any database connections are established
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        
        // Verify timezone is set correctly
        System.out.println("Application Timezone: " + TimeZone.getDefault().getID());
        
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}