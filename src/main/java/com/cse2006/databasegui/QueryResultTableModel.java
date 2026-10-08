package com.cse2006.databasegui;

import javax.swing.table.AbstractTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model that holds the result of one query.
 *
 * <p>Column names, column count and row count all come from the JDBC
 * {@link ResultSet}, so a {@link javax.swing.JTable} using this model adapts
 * automatically to every query.</p>
 */
public class QueryResultTableModel extends AbstractTableModel {

    private List<String> columnNames = List.of();
    private List<Object[]> rows = List.of();

    /**
     * Reads the whole result set into this model while the result set is
     * still open.
     *
     * @param resultSet the result set of an executed query
     * @throws SQLException if reading the result set fails
     */
    public void load(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // The column headers come from the result set metadata.
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        // One pass over the result set: each row becomes an Object[]
        // holding one value per column (JDBC column numbers start at 1).
        List<Object[]> data = new ArrayList<>();
        while (resultSet.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = resultSet.getObject(i);
            }
            data.add(row);
        }

        setResults(columns, data);
    }

    /**
     * Replaces the whole content of this model and tells the attached JTable
     * that the columns (and therefore the rows) have changed.
     *
     * @param columnNames the column headers of the new content
     * @param rows        the rows of the new content
     */
    public void setResults(List<String> columnNames, List<Object[]> rows) {
        this.columnNames = columnNames;
        this.rows = rows;
        fireTableStructureChanged();
    }

    /** Removes all columns and rows from the model. */
    public void clear() {
        setResults(List.of(), List.of());
    }

    /** @return the column headers currently displayed by the model. */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /** @return the rows currently held by the model. */
    public List<Object[]> getRows() {
        return rows;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    /**
     * Returns the type of the first non-empty cell of the column. This makes
     * the JTable sort numbers as numbers instead of as text.
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        for (Object[] row : rows) {
            if (row[columnIndex] != null) {
                return row[columnIndex].getClass();
            }
        }
        return Object.class;
    }
}
