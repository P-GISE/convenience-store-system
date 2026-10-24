package com.example.ordersystem2.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@Profile("prod")
public class ProductionConfiguration {
    @Bean
    static BeanFactoryPostProcessor productionSafetyChecks(Environment environment) {
        // Check before any datasource or schema initializer can be instantiated.
        return beanFactory -> validate(environment);
    }

    private static void validate(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("demo"))) {
            throw new IllegalStateException("prod cannot run with demo");
        }
        for (String key : new String[]{"DB_URL", "DB_USERNAME", "DB_PASSWORD"}) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(key + " is required in prod");
            }
        }
        if (!environment.getRequiredProperty("spring.datasource.url").startsWith("jdbc:mysql:")) {
            throw new IllegalStateException("prod requires MySQL persistence");
        }
        require(environment, "spring.jpa.hibernate.ddl-auto", "validate");
        require(environment, "spring.sql.init.mode", "never");
        require(environment, "spring.session.jdbc.initialize-schema", "never");
    }

    private static void require(Environment environment, String key, String expected) {
        if (!expected.equals(environment.getProperty(key))) {
            throw new IllegalStateException(key + " must be " + expected + " in prod");
        }
    }
}
