package com.cse2006.databasegui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.AbstractAction;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * The complete graphical user interface of the application: query editor,
 * result table, filter controls and status bar.
 *
 * <p>Query execution runs on a {@link SwingWorker} so that the window never
 * freezes while MySQL works.</p>
 */
public class MainFrame extends JFrame {

    private static final String WINDOW_TITLE = "Display Query Results - CSE2006";

    /** Query that is loaded when the application starts (assignment example). */
    private static final String DEFAULT_QUERY = """
            SELECT firstName, lastName, title, editionNumber
            FROM authors
            INNER JOIN authorISBN
            ON authors.authorID = authorISBN.authorID
            INNER JOIN titles
            ON authorISBN.isbn = titles.isbn;""";

    private static final String QUERY_ALL_AUTHORS = "SELECT * FROM authors;";

    private static final String QUERY_ALL_TITLES = "SELECT * FROM titles;";

    private static final String QUERY_JAVA_BOOKS = """
            SELECT firstName, lastName, title, editionNumber
            FROM authors
            INNER JOIN authorISBN
            ON authors.authorID = authorISBN.authorID
            INNER JOIN titles
            ON authorISBN.isbn = titles.isbn
            WHERE title LIKE '%Java%';""";

    private static final String[] EXAMPLE_NAMES = {
            "Select an example...",
            "1. Display All Authors",
            "2. Display Books",
            "3. Authors and Books",
            "4. Java Books"
    };

    private static final Color ACCENT = new Color(41, 87, 154);
    private static final Color TITLE_COLOR = new Color(31, 56, 100);
    private static final Color ALTERNATING_ROW = new Color(240, 245, 252);
    private static final Color STATUS_BACKGROUND = new Color(244, 246, 250);
    private static final Color TEXT_NORMAL = new Color(55, 62, 72);
    private static final Color TEXT_ERROR = new Color(176, 42, 42);
    private static final Color BORDER_COLOR = new Color(206, 214, 226);
    private static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private final QueryController controller = new QueryController();
    private final QueryResultTableModel tableModel = new QueryResultTableModel();
    private final TableRowSorter<QueryResultTableModel> rowSorter =
            new TableRowSorter<>(tableModel);

    private JTextArea queryArea;
    private JTextField filterField;
    private JTable resultTable;
    private JButton submitButton;
    private JComboBox<String> exampleComboBox;
    private JLabel statusLabel;
    private DefaultTableCellRenderer stripedRenderer;
    private SwingWorker<QueryResultTableModel, Void> queryWorker;

    public MainFrame() {
        super(WINDOW_TITLE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1040, 700);
        setMinimumSize(new Dimension(880, 600));
        setLocationRelativeTo(null);

        buildUi();
        installKeyBindings();
        queryArea.setText(DEFAULT_QUERY);
        queryArea.setCaretPosition(0);
        setStatus("Ready", TEXT_NORMAL);
        checkConnection();
    }

    // ------------------------------------------------------------------
    // User interface construction
    // ------------------------------------------------------------------

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(new EmptyBorder(12, 14, 10, 14));
        setContentPane(content);

        content.add(createQueryPanel(), BorderLayout.NORTH);
        content.add(createResultsPanel(), BorderLayout.CENTER);
        content.add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));

        JPanel header = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("SQL Query");
        titleLabel.setFont(BASE_FONT.deriveFont(Font.BOLD, 17f));
        titleLabel.setForeground(TITLE_COLOR);

        JPanel exampleRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        exampleRow.setOpaque(false);
        JLabel exampleLabel = new JLabel("Example Queries:");
        exampleLabel.setFont(BASE_FONT);
        exampleComboBox = new JComboBox<>(EXAMPLE_NAMES);
        exampleComboBox.setFont(BASE_FONT);
        exampleComboBox.addActionListener(e -> loadSelectedExample());
        exampleRow.add(exampleLabel);
        exampleRow.add(exampleComboBox);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(exampleRow, BorderLayout.EAST);

        queryArea = new JTextArea(7, 60);
        queryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        queryArea.setLineWrap(true);
        queryArea.setWrapStyleWord(false);
        queryArea.setTabSize(4);
        queryArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        queryArea.setToolTipText("Type a SELECT query here (Ctrl+Enter runs it)");

        JScrollPane queryScroll = new JScrollPane(queryArea);
        queryScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        submitButton = createButton("Submit Query", true);
        submitButton.setToolTipText("Execute the query (Ctrl+Enter)");
        JButton clearResultsButton = createButton("Clear Results", false);
        submitButton.addActionListener(e -> submitQuery());
        clearResultsButton.addActionListener(e -> clearResults());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(submitButton);
        buttons.add(clearResultsButton);

        panel.add(header, BorderLayout.NORTH);
        panel.add(queryScroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createResultsPanel() {
        resultTable = new JTable(tableModel);
        resultTable.setFont(BASE_FONT);
        resultTable.setRowHeight(24);
        resultTable.setFillsViewportHeight(true);
        resultTable.setGridColor(BORDER_COLOR);
        resultTable.setSelectionBackground(new Color(206, 224, 247));
        resultTable.setSelectionForeground(Color.BLACK);
        resultTable.setRowSorter(rowSorter);

        stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    component.setBackground(row % 2 == 0 ? Color.WHITE : ALTERNATING_ROW);
                }
                setHorizontalAlignment(value instanceof Number
                        ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return component;
            }
        };

        styleTableHeader();

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(titledBorder("Query Results"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void styleTableHeader() {
        JTableHeader header = resultTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(BASE_FONT.deriveFont(Font.BOLD, 13f));
        header.setPreferredSize(new Dimension(
                header.getPreferredSize().width, 30));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(ACCENT);
                label.setForeground(Color.WHITE);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(120, 150, 200)),
                        new EmptyBorder(4, 8, 4, 8)));
                return label;
            }
        });
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));

        JPanel filterRow = new JPanel(new BorderLayout(8, 0));
        JLabel filterLabel = new JLabel("Enter filter text:");
        filterLabel.setFont(BASE_FONT.deriveFont(Font.BOLD));

        filterField = new JTextField();
        filterField.setFont(BASE_FONT);
        filterField.setToolTipText(
                "Text that must appear in a displayed cell (case-insensitive)");
        filterField.addActionListener(e -> applyFilter());

        JButton applyFilterButton = createButton("Apply Filter", true);
        JButton clearFilterButton = createButton("Clear Filter", false);
        applyFilterButton.addActionListener(e -> applyFilter());
        clearFilterButton.addActionListener(e -> clearFilter());

        JPanel filterButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterButtons.setOpaque(false);
        filterButtons.add(applyFilterButton);
        filterButtons.add(clearFilterButton);

        filterRow.add(filterLabel, BorderLayout.WEST);
        filterRow.add(filterField, BorderLayout.CENTER);
        filterRow.add(filterButtons, BorderLayout.EAST);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(BASE_FONT);
        statusLabel.setForeground(TEXT_NORMAL);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(STATUS_BACKGROUND);
        statusLabel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(7, 4, 7, 4)));

        panel.add(filterRow, BorderLayout.NORTH);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JButton createButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(BASE_FONT.deriveFont(primary ? Font.BOLD : Font.PLAIN, 13f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT : BORDER_COLOR),
                new EmptyBorder(6, 14, 6, 14)));
        return button;
    }

    private TitledBorder titledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                BASE_FONT.deriveFont(Font.BOLD, 13f),
                TITLE_COLOR);
    }

    private void installKeyBindings() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                        "submitQuery");
        getRootPane().getActionMap().put("submitQuery", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                submitQuery();
            }
        });
    }

    // ------------------------------------------------------------------
    // Query execution
    // ------------------------------------------------------------------

    private void submitQuery() {
        String sql = queryArea.getText();
        String validationError = controller.validate(sql);
        if (validationError != null) {
            showError(validationError);
            return;
        }
        if (queryWorker != null && !queryWorker.isDone()) {
            return;
        }

        submitButton.setEnabled(false);
        setBusy(true);
        setStatus("Executing query ...", TEXT_NORMAL);

        queryWorker = new SwingWorker<QueryResultTableModel, Void>() {
            @Override
            protected QueryResultTableModel doInBackground() throws Exception {
                return controller.runQuery(sql);
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                setBusy(false);
                try {
                    showResults(get());
                    setStatus("Query executed successfully — "
                            + tableModel.getRowCount() + " rows", TEXT_NORMAL);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    setStatus("Query was interrupted", TEXT_ERROR);
                } catch (ExecutionException ex) {
                    handleFailure(ex.getCause() == null ? ex : ex.getCause());
                } catch (java.util.concurrent.CancellationException ex) {
                    setStatus("Query cancelled", TEXT_NORMAL);
                }
            }
        };
        queryWorker.execute();
    }

    private void showResults(QueryResultTableModel result) {
        tableModel.setResults(result.getColumnNames(), result.getRows());
        filterField.setText("");
        rowSorter.setRowFilter(null);
        applyColumnRendering();
    }

    /**
     * Attaches the striped cell renderer to every column and sizes the columns
     * to their content.
     */
    private void applyColumnRendering() {
        TableColumnModel columns = resultTable.getColumnModel();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            columns.getColumn(i).setCellRenderer(stripedRenderer);
        }

        List<String> names = tableModel.getColumnNames();
        List<Object[]> rows = tableModel.getRows();
        for (int i = 0; i < columns.getColumnCount(); i++) {
            int longest = i < names.size() ? names.get(i).length() : 0;
            for (Object[] row : rows) {
                if (row[i] != null) {
                    longest = Math.max(longest, String.valueOf(row[i]).length());
                }
            }
            int width = Math.min(Math.max(longest * 8 + 24, 100), 420);
            columns.getColumn(i).setPreferredWidth(width);
        }
    }

    private void handleFailure(Throwable cause) {
        String message;
        if (cause instanceof IllegalArgumentException) {
            message = cause.getMessage();
        } else if (cause instanceof SQLException sqlException) {
            message = controller.describeError(sqlException);
        } else {
            message = "Unexpected error: " + cause;
        }
        setStatus("Query failed", TEXT_ERROR);
        showError(message);
    }

    private void setBusy(boolean busy) {
        Cursor cursor = Cursor.getPredefinedCursor(
                busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR);
        getRootPane().setCursor(cursor);
        queryArea.setCursor(cursor);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "SQL Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Filtering
    // ------------------------------------------------------------------

    /**
     * Builds the filter that keeps only rows containing the given text in one
     * of their visible cells. Matching is case-insensitive and literal, so
     * characters such as {@code +} or {@code (} are searched for as they are.
     *
     * @param text the text entered in the filter field
     * @return the matching filter, or {@code null} when the text is empty
     */
    static RowFilter<QueryResultTableModel, Integer> createFilter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim()));
    }

    private void applyFilter() {
        String text = filterField.getText().trim();
        RowFilter<QueryResultTableModel, Integer> filter = createFilter(text);
        rowSorter.setRowFilter(filter);

        int shown = resultTable.getRowCount();
        if (filter == null) {
            setStatus("Filter cleared — " + shown + " rows shown", TEXT_NORMAL);
        } else if (shown == 0) {
            setStatus("Filter applied — no rows match \"" + text + "\"", TEXT_NORMAL);
        } else {
            setStatus("Filter applied — " + shown + " rows shown", TEXT_NORMAL);
        }
    }

    private void clearFilter() {
        filterField.setText("");
        rowSorter.setRowFilter(null);
        setStatus("Filter cleared — " + resultTable.getRowCount()
                + " rows shown", TEXT_NORMAL);
    }

    private void clearResults() {
        tableModel.clear();
        filterField.setText("");
        rowSorter.setRowFilter(null);
        setStatus("Results cleared", TEXT_NORMAL);
    }

    // ------------------------------------------------------------------
    // Example queries and status bar
    // ------------------------------------------------------------------

    private void loadSelectedExample() {
        int index = exampleComboBox.getSelectedIndex();
        if (index <= 0) {
            return;
        }
        String sql = switch (index) {
            case 1 -> QUERY_ALL_AUTHORS;
            case 2 -> QUERY_ALL_TITLES;
            case 3 -> DEFAULT_QUERY;
            case 4 -> QUERY_JAVA_BOOKS;
            default -> null;
        };
        if (sql == null) {
            return;
        }
        queryArea.setText(sql);
        queryArea.setCaretPosition(0);
        exampleComboBox.setSelectedIndex(0);
        setStatus("Example query loaded — press Submit Query", TEXT_NORMAL);
    }

    private void setStatus(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
    }

    private void setStatus(String text, boolean error) {
        setStatus(text, error ? TEXT_ERROR : TEXT_NORMAL);
    }

    /**
     * Checks the database connection in the background. If the schema is
     * missing it is created (with the sample data) by
     * {@link DatabaseInitializer}.
     */
    private void checkConnection() {
        setStatus("Connecting to database ...", TEXT_NORMAL);

        new SwingWorker<StatusUpdate, Void>() {
            @Override
            protected StatusUpdate doInBackground() {
                try {
                    if (DatabaseInitializer.tablesExist()) {
                        return new StatusUpdate(false,
                                "Connected to database — " + DatabaseConnection.describe(),
                                "");
                    }
                    boolean created = DatabaseInitializer.initializeIfMissing();
                    String message = created
                            ? "Database created with sample data — "
                                    + DatabaseConnection.describe()
                            : "Connected to database — " + DatabaseConnection.describe();
                    return new StatusUpdate(false, message, "");
                } catch (SQLException ex) {
                    String details = controller.describeError(ex);
                    return new StatusUpdate(true, firstLine(details), details);
                } catch (Exception ex) {
                    String details = "Could not initialise the database:\n" + ex.getMessage();
                    return new StatusUpdate(true, firstLine(details), details);
                }
            }

            @Override
            protected void done() {
                try {
                    StatusUpdate update = get();
                    setStatus(update.statusText(), update.error());
                    if (update.error()) {
                        JOptionPane.showMessageDialog(MainFrame.this, update.details(),
                                "Database Connection", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    setStatus("Could not connect to the database", true);
                }
            }
        }.execute();
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    /** Result of the background connection check. */
    private record StatusUpdate(boolean error, String statusText, String details) {
    }
}
