package com.sipm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SipmApplication {
    public static void main(String[] args) {
        SpringApplication.run(SipmApplication.class, args);
    }
}
