package com.queueapp.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Hands out JDBC connections built from src/main/resources/db.properties.
 * Kept deliberately simple (no pooling) - swapping this for a
 * javax.sql.DataSource / connection pool is a natural "next improvement"
 * to mention if asked about scaling this project.
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException(
                        "db.properties not found on the classpath. " +
                        "Make sure src/main/resources/db.properties exists and is filled in.");
            }
            PROPS.load(input);
            Class.forName(PROPS.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to initialize database configuration", e);
        }
    }

    private DBConnection() {
        // utility class - no instances
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.username"),
                PROPS.getProperty("db.password")
        );
    }
}
