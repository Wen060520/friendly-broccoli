package com.example.jdbc.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Reads database settings from db.properties and allows environment overrides.
 */
public final class DatabaseConfig {
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("DB_URL", "jdbc.url");
        String username = setting("DB_USERNAME", "jdbc.username");
        String password = setting("DB_PASSWORD", "jdbc.password");
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String environmentName, String propertyName) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return PROPERTIES.getProperty(propertyName);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("db.properties was not found");
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load db.properties", exception);
        }
    }
}
