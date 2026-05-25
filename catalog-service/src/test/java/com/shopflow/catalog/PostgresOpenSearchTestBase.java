package com.shopflow.catalog;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
public abstract class PostgresOpenSearchTestBase {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_catalogservice_db")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> opensearch = new GenericContainer<>("opensearchproject/opensearch:2.11.0")
            .withEnv("discovery.type", "single-node")
            .withEnv("DISABLE_SECURITY_PLUGIN", "true")
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms256m -Xmx256m")
            .withExposedPorts(9200)
            .waitingFor(Wait.forHttp("/_cluster/health")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofSeconds(120)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.opensearch.uris",
                () -> "http://" + opensearch.getHost() + ":" + opensearch.getMappedPort(9200));
    }
}
