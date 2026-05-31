package com.example.canteensimulation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CanteenSimulationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanteenSimulationApplication.class, args);
    }

}
