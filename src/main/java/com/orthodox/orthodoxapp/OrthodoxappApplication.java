package com.orthodox.orthodoxapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The starting point of the whole backend.
 *
 * When you press "Run", Java calls the main() method below. That one line
 * starts Spring Boot, which then:
 *   1. Scans this package (com.orthodox.orthodoxapp) and everything inside it
 *      for classes marked with annotations like @RestController.
 *   2. Creates those classes for us (we never write "new TodayController()").
 *   3. Starts a web server on http://localhost:8080 and waits for requests.
 *
 * @SpringBootApplication is a shortcut that switches all of that on.
 * Every other class must live in this package (or a sub-package) so Spring finds it.
 */
@SpringBootApplication
public class OrthodoxappApplication {

    public static void main(String[] args) {
        // Online, the host gives us the database's address as DATABASE_URL;
        // turn it into settings Spring Boot understands (see DatabaseUrl.java).
        DatabaseUrl.useIfPresent();

        // Hand control to Spring Boot. "args" are any command-line options.
        SpringApplication.run(OrthodoxappApplication.class, args);
    }
}
