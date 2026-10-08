package com.cse2006.databasegui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Holds the JDBC settings and opens database connections.
 *
 * <p>Every value can be overridden without touching the code. The order used
 * for each setting is:</p>
 * <ol>
 *   <li>JVM system property, e.g. {@code -DDB_USER=root}</li>
 *   <li>environment variable, e.g. {@code DB_PASSWORD=secret}</li>
 *   <li>the default constant below</li>
 * </ol>
 *
 * <p>Supported keys: {@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}.</p>
 */
public final class DatabaseConnection {

    /**
     * Default JDBC URL of the course database. Change the host, port or
     * database name here if your MySQL installation is different.
     */
    public static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/cse2006_library"
                    + "?sslMode=DISABLED&allowPublicKeyRetrieval=true";

    /** Default database user. */
    public static final String DEFAULT_DB_USER = "root";

    /**
     * Default password. It is empty because a fresh MySQL install created by
     * {@code mysqld --initialize-insecure} has no root password.
     *
     * <p>Change it locally or set the {@code DB_PASSWORD} environment
     * variable. Never commit a real password to version control.</p>
     */
    public static final String DEFAULT_DB_PASSWORD = "";

    private DatabaseConnection() {
    }

    /** @return the JDBC URL currently in use. */
    public static String getUrl() {
        return resolve("DB_URL", DEFAULT_DB_URL);
    }

    /** @return the database user currently in use. */
    public static String getUser() {
        return resolve("DB_USER", DEFAULT_DB_USER);
    }

    /** @return the database password currently in use. */
    public static String getPassword() {
        return resolve("DB_PASSWORD", DEFAULT_DB_PASSWORD);
    }

    /**
     * Opens a new JDBC connection to the configured database.
     * Callers close it, normally with try-with-resources.
     *
     * @return an open connection
     * @throws SQLException if the URL, credentials or server are wrong
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    /**
     * @return the database name from the JDBC URL, for example
     *         {@code cse2006_library} from
     *         {@code jdbc:mysql://localhost:3306/cse2006_library?...},
     *         or an empty string when the URL contains none
     */
    public static String getDatabaseName() {
        String url = getUrl();
        int start = databaseStart(url);
        if (start < 0) {
            return "";
        }
        String name = url.substring(start);
        int query = name.indexOf('?');
        return query < 0 ? name : name.substring(0, query);
    }

    /**
     * @return the JDBC URL of the MySQL server itself, that is the configured
     *         URL without the database name — needed to run CREATE DATABASE
     */
    public static String getServerUrl() {
        String url = getUrl();
        int start = databaseStart(url);
        if (start < 0) {
            return url;
        }
        String server = url.substring(0, start - 1);
        int query = url.indexOf('?');
        return query < 0 ? server : server + url.substring(query);
    }

    /**
     * @return short, password-free description of the configuration for the
     *         status bar, e.g. {@code root -> cse2006_library}
     */
    public static String describe() {
        return getUser() + " -> " + getDatabaseName();
    }

    /**
     * In a URL such as {@code jdbc:mysql://localhost:3306/cse2006_library}
     * this finds the "/" that starts the database name.
     *
     * @return index of the first character of the database name, or {@code -1}
     *         when the URL has no database name
     */
    private static int databaseStart(String url) {
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return -1;
        }
        int slash = url.indexOf('/', scheme + 3);
        return slash < 0 ? -1 : slash + 1;
    }

    /**
     * Reads one setting: system property first, then environment variable,
     * then the default constant.
     */
    private static String resolve(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value == null ? fallback : value;
    }
}
