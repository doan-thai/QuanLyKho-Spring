package com.syncstock.common.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfrastructureConfigTest {

    private static Path projectRoot;
    private static String dockerComposeContent;
    private static String dockerComposeDevContent;
    private static String initSqlContent;
    private static String envExampleContent;

    @BeforeAll
    static void setUp() throws IOException {
        Path current = Path.of("").toAbsolutePath();
        if (Files.exists(current.resolve("docker-compose.yml"))) {
            projectRoot = current;
        } else if (Files.exists(current.resolve("../docker-compose.yml"))) {
            projectRoot = current.resolve("..").normalize();
        } else {
            throw new IllegalStateException("Cannot find project root with docker-compose.yml from " + current);
        }

        dockerComposeContent = Files.readString(projectRoot.resolve("docker-compose.yml"));
        dockerComposeDevContent = Files.readString(projectRoot.resolve("docker-compose.dev.yml"));
        initSqlContent = Files.readString(projectRoot.resolve("scripts/init-mysql.sql"));
        envExampleContent = Files.readString(projectRoot.resolve(".env.example"));
    }

    @Test
    @DisplayName("docker-compose.yml: Chỉ api-gateway (5000) publish port ra host; Bỏ publish 5001-5005")
    void testDockerComposeOnlyApiGatewayExposed() {
        // api-gateway must publish port 5000:8080
        assertTrue(dockerComposeContent.contains("\"5000:8080\""),
                "api-gateway must expose port 5000:8080");

        // Internal microservices must NOT publish ports 5001-5005 to host
        assertFalse(dockerComposeContent.contains("\"5001:8080\""), "auth-service port 5001 must not be published");
        assertFalse(dockerComposeContent.contains("\"5002:8080\""), "inventory-service port 5002 must not be published");
        assertFalse(dockerComposeContent.contains("\"5003:8080\""), "supplier-service port 5003 must not be published");
        assertFalse(dockerComposeContent.contains("\"5004:8080\""), "report-service port 5004 must not be published");
        assertFalse(dockerComposeContent.contains("\"5005:8080\""), "ai-service port 5005 must not be published");

        // Base docker-compose.yml must not expose MySQL (3307) or RabbitMQ (5672, 15672)
        assertFalse(dockerComposeContent.contains("\"3307:3306\""), "MySQL port 3307 must not be published in base docker-compose");
        assertFalse(dockerComposeContent.contains("\"5672:5672\""), "RabbitMQ port 5672 must not be published in base docker-compose");
        assertFalse(dockerComposeContent.contains("\"15672:15672\""), "RabbitMQ port 15672 must not be published in base docker-compose");
    }

    @Test
    @DisplayName("docker-compose.dev.yml: MySQL (3307) và RabbitMQ (5672, 15672) được cấu hình cho dev override")
    void testDockerComposeDevOverride() {
        assertTrue(dockerComposeDevContent.contains("\"3307:3306\""),
                "docker-compose.dev.yml must expose MySQL port 3307:3306");
        assertTrue(dockerComposeDevContent.contains("\"5672:5672\""),
                "docker-compose.dev.yml must expose RabbitMQ port 5672:5672");
        assertTrue(dockerComposeDevContent.contains("\"15672:15672\""),
                "docker-compose.dev.yml must expose RabbitMQ port 15672:15672");
    }

    @Test
    @DisplayName("scripts/init-mysql.sql: Tạo user riêng biệt cho từng service (auth_user, inventory_user, supplier_user) với đặc quyền tối thiểu")
    void testInitMySqlUserPrivileges() {
        // auth_user must be created and granted ONLY on auth_db
        assertTrue(initSqlContent.contains("'auth_user'@'%'"), "Must create auth_user");
        assertTrue(initSqlContent.contains("GRANT ALL PRIVILEGES ON `auth_db`.* TO 'auth_user'@'%'"),
                "auth_user must have privileges on auth_db");
        assertFalse(initSqlContent.contains("GRANT ALL PRIVILEGES ON `warehouse_db`.* TO 'auth_user'@'%'"),
                "auth_user must NOT have privileges on warehouse_db");
        assertFalse(initSqlContent.contains("GRANT ALL PRIVILEGES ON `supplier_db`.* TO 'auth_user'@'%'"),
                "auth_user must NOT have privileges on supplier_db");

        // inventory_user must be created and granted ONLY on warehouse_db
        assertTrue(initSqlContent.contains("'inventory_user'@'%'"), "Must create inventory_user");
        assertTrue(initSqlContent.contains("GRANT ALL PRIVILEGES ON `warehouse_db`.* TO 'inventory_user'@'%'"),
                "inventory_user must have privileges on warehouse_db");
        assertFalse(initSqlContent.contains("GRANT ALL PRIVILEGES ON `auth_db`.* TO 'inventory_user'@'%'"),
                "inventory_user must NOT have privileges on auth_db");

        // supplier_user must be created and granted ONLY on supplier_db
        assertTrue(initSqlContent.contains("'supplier_user'@'%'"), "Must create supplier_user");
        assertTrue(initSqlContent.contains("GRANT ALL PRIVILEGES ON `supplier_db`.* TO 'supplier_user'@'%'"),
                "supplier_user must have privileges on supplier_db");
        assertFalse(initSqlContent.contains("GRANT ALL PRIVILEGES ON `auth_db`.* TO 'supplier_user'@'%'"),
                "supplier_user must NOT have privileges on auth_db");
    }

    @Test
    @DisplayName("docker-compose.yml & .env.example: Datasource không dùng root cho microservices")
    void testDatasourceConfigDoesNotUseRoot() {
        // In docker-compose.yml, services must not use root
        assertFalse(dockerComposeContent.contains("SPRING_DATASOURCE_USERNAME=root"),
                "docker-compose.yml must not use root username for services");
        assertTrue(dockerComposeContent.contains("SPRING_DATASOURCE_USERNAME=${AUTH_DB_USER:-auth_user}"),
                "auth-service must use auth_user");
        assertTrue(dockerComposeContent.contains("SPRING_DATASOURCE_USERNAME=${INVENTORY_DB_USER:-inventory_user}"),
                "inventory-service must use inventory_user");
        assertTrue(dockerComposeContent.contains("SPRING_DATASOURCE_USERNAME=${SUPPLIER_DB_USER:-supplier_user}"),
                "supplier-service must use supplier_user");

        // In .env.example, service user/pass must be documented
        assertTrue(envExampleContent.contains("AUTH_DB_USER=auth_user"), ".env.example must specify AUTH_DB_USER");
        assertTrue(envExampleContent.contains("INVENTORY_DB_USER=inventory_user"), ".env.example must specify INVENTORY_DB_USER");
        assertTrue(envExampleContent.contains("SUPPLIER_DB_USER=supplier_user"), ".env.example must specify SUPPLIER_DB_USER");
    }
}

