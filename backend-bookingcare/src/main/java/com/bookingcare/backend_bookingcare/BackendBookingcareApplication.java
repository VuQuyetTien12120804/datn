package com.bookingcare.backend_bookingcare;

import com.bookingcare.backend_bookingcare.config.MailProperties;
import com.bookingcare.backend_bookingcare.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, MailProperties.class})
public class BackendBookingcareApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendBookingcareApplication.class, args);
	}

}
