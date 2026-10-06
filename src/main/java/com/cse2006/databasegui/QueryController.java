package com.cse2006.databasegui;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Executes the SQL that is typed into the GUI.
 *
 * <p>This class owns everything that happens between the text area and the
 * table: validation of the submitted text, the JDBC call itself and the
 * translation of technical SQL exceptions into friendly messages.</p>
 *
 * <p>Only single {@code SELECT} statements are accepted, so the demonstration
 * cannot accidentally change or destroy data.</p>
 */
public class QueryController {

    /** A statement must start with SELECT (case-insensitive). */
    private static final Pattern SELECT_STATEMENT =
            Pattern.compile("\\A\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Validates and executes a query.
     *
     * @param sql the SQL text entered by the user
     * @return a table model holding all result rows
     * @throws IllegalArgumentException if the SQL is empty or not allowed
     * @throws SQLException             if the database rejects the query
     */
    public QueryResultTableModel runQuery(String sql) throws SQLException {
        String validationError = validate(sql);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            QueryResultTableModel model = new QueryResultTableModel();
            model.load(resultSet);
            return model;
        }
    }

    /**
     * Checks whether the submitted text may be sent to the database.
     *
     * @param sql the SQL text entered by the user
     * @return {@code null} when the text is acceptable, otherwise a friendly
     *         message that can be shown in a dialog
     */
    public String validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return "Please enter a SQL query first.";
        }

        String statement = stripLeadingComments(sql).trim();
        if (statement.endsWith(";")) {
            statement = statement.substring(0, statement.length() - 1).trim();
        }
        if (statement.isEmpty()) {
            return "Please enter a SQL query first.";
        }
        if (statement.contains(";")) {
            return "Please submit one SQL statement at a time.";
        }
        if (!SELECT_STATEMENT.matcher(statement).find()) {
            return "Only read-only SELECT queries are allowed in this "
                    + "demonstration application.\n"
                    + "Statements such as DROP, DELETE, UPDATE, ALTER or "
                    + "TRUNCATE are blocked.";
        }
        return null;
    }

    /**
     * Translates a JDBC exception into a message a student can act on.
     *
     * @param exception the exception thrown while running the query
     * @return a friendly, multi-line description of the problem
     */
    public String describeError(SQLException exception) {
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("unknown database")) {
            return "The database does not exist yet.\n"
                    + "Run database/database.sql in MySQL Workbench first.\n\n"
                    + message;
        }
        if (lower.contains("access denied")) {
            return "Database login failed.\n"
                    + "Check DB_USER and DB_PASSWORD (see README, section 8).\n\n"
                    + message;
        }
        if (lower.contains("communications link failure")
                || lower.contains("connection refused")
                || lower.contains("connect timed out")
                || lower.contains("no suitable driver")) {
            return "Cannot reach the MySQL server.\n"
                    + "Is MySQL running on localhost:3306?\n\n"
                    + message;
        }
        if (lower.contains("doesn't exist")
                || lower.contains("unknown table")
                || lower.contains("unknown column")) {
            return "A table or column does not exist.\n"
                    + "Check the spelling in your query, or run database/database.sql "
                    + "if the sample schema is missing.\n\n"
                    + message;
        }
        return "The query could not be executed:\n\n" + message;
    }

    /**
     * Removes {@code --}, {@code #} and {@code /* ... *}{@code /} comments that
     * appear before the first SQL keyword so that a commented line does not
     * make a valid query look invalid.
     */
    private String stripLeadingComments(String sql) {
        String rest = sql;
        boolean changed = true;
        while (changed) {
            changed = false;
            rest = rest.trim();
            if (rest.startsWith("--") || rest.startsWith("#")) {
                int newline = rest.indexOf('\n');
                rest = newline < 0 ? "" : rest.substring(newline + 1);
                changed = true;
            } else if (rest.startsWith("/*")) {
                int end = rest.indexOf("*/");
                rest = end < 0 ? "" : rest.substring(end + 2);
                changed = true;
            }
        }
        return rest;
    }
}
