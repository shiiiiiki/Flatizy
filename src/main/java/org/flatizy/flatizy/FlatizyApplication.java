package org.flatizy.flatizy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlatizyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlatizyApplication.class, args);
        System.out.println("FlatizyApplication started");
    }

}
