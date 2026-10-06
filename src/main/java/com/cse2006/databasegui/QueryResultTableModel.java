package com.cse2006.databasegui;

import javax.swing.table.AbstractTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Table model that can display the contents of an arbitrary JDBC
 * {@link ResultSet}.
 *
 * <p>Column names, the number of columns and the number of rows are taken from
 * the result set itself, so a {@link javax.swing.JTable} using this model adapts
 * automatically to every query.</p>
 */
public class QueryResultTableModel extends AbstractTableModel {

    private List<String> columnNames = new ArrayList<>();
    private List<Object[]> rows = new ArrayList<>();

    /**
     * Reads the complete result set (metadata and rows) into this model.
     * Must be called while the result set is still open.
     *
     * @param resultSet the result set of an executed query
     * @throws SQLException if reading the result set fails
     */
    public void load(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

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
     * Replaces the whole content of this model and notifies attached tables.
     *
     * @param columns column headers of the new content
     * @param data    the rows of the new content
     */
    public void setResults(List<String> columns, List<Object[]> data) {
        columnNames = new ArrayList<>(columns);
        rows = new ArrayList<>(data);
        fireTableStructureChanged();
    }

    /** Removes all rows and columns from the model. */
    public void clear() {
        setResults(Collections.emptyList(), Collections.emptyList());
    }

    /** @return the column headers currently displayed by the model. */
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }

    /** @return the rows currently held by the model. */
    public List<Object[]> getRows() {
        return Collections.unmodifiableList(rows);
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
     * Returns the type of the first non-empty value of the column. Numeric
     * columns are sorted as numbers instead of as text.
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
