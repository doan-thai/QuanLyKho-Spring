package com.syncstock.inventory;

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
    @DisplayName("Flyway executes V1__baseline.sql on clean schema and creates inventory tables")
    void testFlywayMigrationCleanDb() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:inventory_flyway_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
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
             Statement stmt = conn.createStatement()) {
            assertTableExists(stmt, "categories");
            assertTableExists(stmt, "products");
            assertTableExists(stmt, "import_receipts");
            assertTableExists(stmt, "export_receipts");
            assertTableExists(stmt, "stock_transactions");
        }
    }

    @Test
    @DisplayName("Flyway baselineOnMigrate skips V1 if schema already exists")
    void testFlywayBaselineOnMigrateExistingDb() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:inventory_baseline_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        // Simulate existing pre-Flyway tables
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE categories (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL)");
            stmt.execute("INSERT INTO categories (name) VALUES ('Điện tử')");
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
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    private void assertTableExists(Statement stmt, String tableName) throws Exception {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}

