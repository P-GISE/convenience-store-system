package com.example.ordersystem2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Profiles;
import com.example.ordersystem2.service.OperatorBootstrapService;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableJpaAuditing
@SpringBootApplication
public class Ordersystem2Application {

	public static void main(String[] args) {
		if ("true".equals(System.getenv("STORE_BOOTSTRAP"))) {
			try (var context = new SpringApplicationBuilder(Ordersystem2Application.class)
					.web(WebApplicationType.NONE).run(args)) {
				if (!context.getEnvironment().acceptsProfiles(Profiles.of("prod"))) {
					throw new IllegalStateException("production_profile_required");
				}
				context.getBean(OperatorBootstrapService.class).apply(new ObjectMapper().readTree(System.in));
				System.out.println("operator_bootstrap status=created");
			} catch (Exception error) {
				// Input may contain a password; never print exception messages or the JSON.
				System.err.println("operator_bootstrap status=failed");
				System.exit(1);
			}
			return;
		}
		SpringApplication.run(Ordersystem2Application.class, args);
	}

}

