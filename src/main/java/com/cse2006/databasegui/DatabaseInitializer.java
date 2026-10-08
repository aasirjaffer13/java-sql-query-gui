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

/**
 * Creates the sample schema when it is missing.
 *
 * <p>The script {@code database/database.sql} is packaged into the jar by
 * Maven, so the application can create the database, the tables and the sample
 * data by itself on first start. When everything already exists, nothing is
 * executed at all.</p>
 */
public final class DatabaseInitializer {

    /** Tables the application needs. */
    private static final String[] REQUIRED_TABLES = { "authors", "titles", "authorISBN" };

    /** The packaged script, found on the classpath. */
    private static final String SCRIPT_RESOURCE = "/database.sql";

    private DatabaseInitializer() {
    }

    /**
     * Checks whether the database exists and all required tables are present.
     *
     * @return {@code true} when every table exists
     * @throws SQLException if the server cannot be reached or the login fails
     */
    public static boolean tablesExist() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return tablesExist(connection);
        } catch (SQLException ex) {
            // "Unknown database" only means the database has not been created yet,
            // which is not an error for the caller - it will create it next.
            if (ex.getMessage() != null
                    && ex.getMessage().toLowerCase(Locale.ROOT).contains("unknown database")) {
                return false;
            }
            throw ex;
        }
    }

    /**
     * Creates the database and the schema (with sample data) when they are
     * missing.
     *
     * @return {@code true} when the schema had to be created,
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

    /**
     * Connects to the server without a database (the configured database may
     * not exist yet) and creates it if it is missing.
     */
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

    /** Asks MySQL how many of the required tables exist in this database. */
    private static boolean tablesExist(Connection connection) throws SQLException {
        List<String> quotedTables = new ArrayList<>();
        for (String table : REQUIRED_TABLES) {
            quotedTables.add("'" + table + "'");
        }
        String sql = "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() "
                + "AND table_name IN (" + String.join(", ", quotedTables) + ")";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() && resultSet.getInt(1) == REQUIRED_TABLES.length;
        }
    }

    /** Executes the packaged script, one statement at a time. */
    private static void runScript(Connection connection) throws SQLException, IOException {
        for (String sql : splitStatements(readScript())) {
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
     * JDBC executes one statement at a time, so the script is split on ";".
     * Comment lines are dropped, and so are CREATE DATABASE and USE because
     * the application handles those itself.
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
