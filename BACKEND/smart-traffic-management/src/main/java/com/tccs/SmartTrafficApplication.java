package com.tccs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartTrafficApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTrafficApplication.class, args);
        System.out.println("===========================================");
        System.out.println("  TCCS - Traffic Command Control System    ");
        System.out.println("  Server running on http://localhost:8090   ");
        System.out.println("  H2 Console: http://localhost:8090/h2-console");
        System.out.println("===========================================");
    }
}