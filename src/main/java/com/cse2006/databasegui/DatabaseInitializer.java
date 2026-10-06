package com.cse2006.databasegui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Optional helper that makes sure the demonstration schema exists.
 *
 * <p>The SQL script {@code database/database.sql} is packaged as a class
 * resource by Maven, so this class can create the database, the tables and the
 * sample data when they are missing. The application does not depend on it:
 * if the schema is already there, nothing is executed at all.</p>
 */
public final class DatabaseInitializer {

    /** Tables that must exist before the application can be used. */
    private static final String[] REQUIRED_TABLES = { "authors", "titles", "authorISBN" };

    /** Location of the script inside the built application. */
    private static final String SCRIPT_RESOURCE = "/database.sql";

    private DatabaseInitializer() {
    }

    /**
     * Checks whether all required tables are present in the configured
     * database.
     *
     * <p>Returns {@code false} also when the database itself does not exist
     * yet, so callers can simply run {@link #initializeIfMissing()}.</p>
     *
     * @return {@code true} when every table exists
     * @throws SQLException if the database server cannot be reached or the
     *                      credentials are wrong
     */
    public static boolean tablesExist() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return tablesExist(connection);
        } catch (SQLException ex) {
            if (isMissingDatabase(ex)) {
                return false;
            }
            throw ex;
        }
    }

    private static boolean isMissingDatabase(SQLException ex) {
        return ex.getMessage() != null
                && ex.getMessage().toLowerCase(Locale.ROOT).contains("unknown database");
    }

    /**
     * Creates the database and the schema when they are missing.
     *
     * @return {@code true} when the sample schema had to be created,
     *         {@code false} when everything already existed
     * @throws SQLException if the server or the script fails
     * @throws IOException  if the packaged script cannot be read
     */
    public static boolean initializeIfMissing() throws SQLException, IOException {
        createDatabaseIfMissing();

        try (Connection connection = DatabaseConnection.getConnection()) {
            if (tablesExist(connection)) {
                return false;
            }
            runScript(connection);
            return tablesExist(connection);
        }
    }

    private static void createDatabaseIfMissing() throws SQLException {
        String databaseName = DatabaseConnection.getDatabaseName();
        if (databaseName.isEmpty()) {
            throw new SQLException("The DB_URL does not contain a database name.");
        }
        String sql = "CREATE DATABASE IF NOT EXISTS `" + databaseName + "`";

        try (Connection server = DriverManager.getConnection(
                    DatabaseConnection.getServerUrl(),
                    DatabaseConnection.getUser(),
                    DatabaseConnection.getPassword());
             Statement statement = server.createStatement()) {
            statement.execute(sql);
        }
    }

    private static boolean tablesExist(Connection connection) throws SQLException {
        StringJoiner names = new StringJoiner(", ", "(", ")");
        for (String table : REQUIRED_TABLES) {
            names.add("'" + table + "'");
        }
        String sql = "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name IN " + names;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() && resultSet.getInt(1) == REQUIRED_TABLES.length;
        }
    }

    private static void runScript(Connection connection) throws SQLException, IOException {
        String script = readScript();
        for (String sql : splitStatements(script)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private static String readScript() throws IOException {
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(SCRIPT_RESOURCE)) {
            if (in == null) {
                throw new IOException(SCRIPT_RESOURCE
                        + " was not found. Run database/database.sql manually.");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Splits the script on semicolons and drops comments as well as the
     * statements that are already handled by the application itself.
     */
    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        for (String part : script.split(";")) {
            String statement = removeCommentLines(part).trim();
            if (statement.isEmpty() || isConnectionCommand(statement)) {
                continue;
            }
            statements.add(statement);
        }
        return statements;
    }

    private static String removeCommentLines(String text) {
        StringBuilder kept = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }
            kept.append(line).append('\n');
        }
        return kept.toString();
    }

    private static boolean isConnectionCommand(String statement) {
        String upper = statement.toUpperCase(Locale.ROOT);
        return upper.startsWith("CREATE DATABASE") || upper.startsWith("USE ");
    }
}
