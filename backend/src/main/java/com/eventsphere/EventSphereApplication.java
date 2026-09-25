package com.eventsphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class EventSphereApplication {

    public static void main(String[] args) {
        // Event times are local wall-clock times; pin the JVM zone so a UTC cloud server behaves like a laptop.
        String zone = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(EventSphereApplication.class, args);
    }
}
