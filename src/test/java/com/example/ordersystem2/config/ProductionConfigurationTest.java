package com.example.ordersystem2.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationTest {
    private final AtomicBoolean datasourceCreated = new AtomicBoolean();
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                context.getEnvironment().setActiveProfiles("prod");
                try {
                    context.getEnvironment().getPropertySources().addLast(new ResourcePropertySource(
                            new ClassPathResource("application-prod.properties")));
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            })
            .withUserConfiguration(ProductionConfiguration.class, DataInitializer.class)
            .withBean("datasourceSentinel", Object.class, () -> {
                datasourceCreated.set(true);
                return new Object();
            })
            .withPropertyValues("DB_URL=jdbc:mysql://synthetic.invalid/convenience",
                    "DB_USERNAME=synthetic", "DB_PASSWORD=synthetic-test-only");

    @Test
    void productionAllowsValidatedConfigurationWithoutSeed() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean("seedProducts");
            assertThat(datasourceCreated).isTrue();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DB_URL=", "DB_USERNAME=", "DB_PASSWORD=",
            "spring.datasource.url=jdbc:h2:mem:unexpected",
            "spring.jpa.hibernate.ddl-auto=update",
            "spring.sql.init.mode=always",
            "spring.session.jdbc.initialize-schema=always"
    })
    void unsafeConfigurationFailsBeforeDatasourceCreation(String override) {
        runner.withPropertyValues(override).run(context -> {
            assertThat(context).hasFailed();
            assertThat(datasourceCreated).isFalse();
        });
    }

    @Test
    void productionCannotEnableDemoUsers() {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("prod", "demo"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(datasourceCreated).isFalse();
                });
    }
}
