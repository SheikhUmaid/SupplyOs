package com.supplychain;

import com.supplychain.config.RequiredSettingsCheck;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SupplychainBackendApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SupplychainBackendApplication.class);
        application.addListeners(new RequiredSettingsCheck());
        application.run(args);
    }
}
