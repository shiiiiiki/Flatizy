package org.flatizy.flatizy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class FlatizyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlatizyApplication.class, args);
        System.out.println("FlatizyApplication started");
    }

}
