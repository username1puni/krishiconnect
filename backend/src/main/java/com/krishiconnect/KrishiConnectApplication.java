package com.krishiconnect;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KrishiConnectApplication {

    static {
        // The JVM talks to MySQL in UTC so DATETIME columns are never shifted by the server's zone.
        // Booking times are LocalDateTime in IST and "now" comes from the IST Clock bean.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(KrishiConnectApplication.class, args);
    }
}
