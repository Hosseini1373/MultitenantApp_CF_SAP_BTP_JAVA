package com.concircle.multitenantapp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;



import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan
@ServletComponentScan
@EntityScan
@EnableJpaRepositories
@Log4j2



// @EnableWebSecurity( debug = true ) // TODO "debug" may include sensitive information. Do not use in a production system!
// @EnableMethodSecurity(securedEnabled = false, jsr250Enabled = false )
public class MultitenantDemoApplication {
	// static LoggerUtil logger = new LoggerUtil("MultitenantDemoApplication");

	public static void main(String[] args) {
		// check if it is running locally
		// if (!System.getProperty("PRODUCTION").equals("true")) {
		// 	Dotenv dotenv = Dotenv.load();
		// }
        log.info("***************  Welcome to MultitenantDemoApplication application ***************");
		log.info("spring.datasource.url: " + System.getProperty("spring.datasource.url"));
		SpringApplication.run(MultitenantDemoApplication.class, args);
	}

}
