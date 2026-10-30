package com.syncstock.supplier;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest {

    @Test
    @DisplayName("Flyway executes V1__baseline.sql on clean schema and creates suppliers table")
    void testFlywayMigrationCleanDb() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:supplier_flyway_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        MigrateResult result = flyway.migrate();
        assertTrue(result.success);
        assertEquals(1, result.migrationsExecuted);

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM suppliers")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("Flyway baselineOnMigrate skips V1 if schema already exists")
    void testFlywayBaselineOnMigrateExistingDb() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:supplier_baseline_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        // Simulate existing pre-Flyway tables
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE suppliers (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(200) NOT NULL, " +
                    "contact_person VARCHAR(50), " +
                    "phone VARCHAR(20), " +
                    "email VARCHAR(150), " +
                    "address VARCHAR(300), " +
                    "is_active BIT(1) NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL, " +
                    "total_transactions INT NOT NULL, " +
                    "total_import_value DECIMAL(18,2) NOT NULL, " +
                    "reliability_score DECIMAL(18,2) NOT NULL, " +
                    "last_transaction_at TIMESTAMP" +
                    ")");
            stmt.execute("INSERT INTO suppliers (name, is_active, created_at, total_transactions, total_import_value, reliability_score) " +
                    "VALUES ('Công ty ABC', 1, CURRENT_TIMESTAMP, 0, 0, 50)");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        MigrateResult result = flyway.migrate();
        assertTrue(result.success);
        assertEquals(0, result.migrationsExecuted);

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM suppliers")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}

