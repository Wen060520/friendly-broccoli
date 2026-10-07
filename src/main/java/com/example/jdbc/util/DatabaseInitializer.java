package com.example.jdbc.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes schema.sql before the CRUD demonstration and tests.
 */
public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize(Connection connection) throws SQLException {
        String script = readSchema();
        for (String sql : script.split(";")) {
            String statementSql = sql.strip();
            if (!statementSql.isEmpty()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementSql);
                }
            }
        }
    }

    public static void clearData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM students");
            statement.executeUpdate("DELETE FROM colleges");
            statement.execute("ALTER TABLE students ALTER COLUMN id RESTART WITH 1");
            statement.execute("ALTER TABLE colleges ALTER COLUMN id RESTART WITH 1");
        }
    }

    private static String readSchema() {
        try (InputStream input = DatabaseInitializer.class.getClassLoader()
                .getResourceAsStream("schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("schema.sql was not found");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read schema.sql", exception);
        }
    }
}
