package fi.seamk.kodera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Kodera REST API.
 */
@SpringBootApplication
public class KoderaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoderaApplication.class, args);
    }
}