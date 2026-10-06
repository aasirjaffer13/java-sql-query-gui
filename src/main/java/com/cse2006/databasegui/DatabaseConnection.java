package com.cse2006.databasegui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single place for the JDBC configuration and for opening connections.
 *
 * <p>The settings can be overridden without changing the code. The order used
 * for every value is:</p>
 * <ol>
 *   <li>JVM system property, e.g. {@code -DDB_USER=root}</li>
 *   <li>environment variable, e.g. {@code DB_PASSWORD=secret}</li>
 *   <li>the clearly marked default constant below</li>
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
     * Default database password. It is empty because a fresh MySQL install
     * created by {@code mysqld --initialize-insecure} has no root password.
     *
     * <p>Change this value locally or set the {@code DB_PASSWORD} environment
     * variable. Never commit a real password to version control.</p>
     */
    public static final String DEFAULT_DB_PASSWORD = "";

    private DatabaseConnection() {
    }

    /** @return the JDBC URL that is currently in use. */
    public static String getUrl() {
        return resolve("DB_URL", DEFAULT_DB_URL);
    }

    /** @return the database user that is currently in use. */
    public static String getUser() {
        return resolve("DB_USER", DEFAULT_DB_USER);
    }

    /** @return the database password that is currently in use. */
    public static String getPassword() {
        return resolve("DB_PASSWORD", DEFAULT_DB_PASSWORD);
    }

    /**
     * Opens a new JDBC connection.
     *
     * <p>Callers must close it, normally with try-with-resources.</p>
     *
     * @return an open connection to the configured database
     * @throws SQLException if the URL, credentials or server are wrong
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    /**
     * JDBC URL of the MySQL server itself, i.e. the configured URL without
     * the database name. Used to create the database when it is missing.
     *
     * @return server level JDBC URL (query parameters are preserved)
     */
    public static String getServerUrl() {
        String url = getUrl();
        int scheme = url.indexOf("://");
        int pathStart = scheme < 0 ? -1 : url.indexOf('/', scheme + 3);
        if (pathStart < 0) {
            return url;
        }
        int query = url.indexOf('?', pathStart);
        return url.substring(0, pathStart) + (query < 0 ? "" : url.substring(query));
    }

    /**
     * @return the database name taken from the JDBC URL, or an empty string
     *         if the URL does not contain one
     */
    public static String getDatabaseName() {
        String url = getUrl();
        int scheme = url.indexOf("://");
        int pathStart = scheme < 0 ? -1 : url.indexOf('/', scheme + 3);
        if (pathStart < 0) {
            return "";
        }
        String path = url.substring(pathStart + 1);
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int slash = path.indexOf('/');
        if (slash >= 0) {
            path = path.substring(0, slash);
        }
        return path;
    }

    /**
     * @return short, password-free description of the configuration, used in
     *         the status bar, e.g. {@code root -> cse2006_library}
     */
    public static String describe() {
        return getUser() + " -> " + getDatabaseName();
    }

    private static String resolve(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null && System.getenv(key) != null) {
            value = System.getenv(key);
        }
        return value == null ? fallback : value;
    }
}
