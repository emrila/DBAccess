package com.emla;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DBModel {

    private static final String PROTOCOL = "jdbc:ucanaccess://";
    private String accessFile;

    private Connection connection;
    private Statement statement;

    private final List<String> tableNames = new ArrayList<>();
    private final List<String> columnNames = new ArrayList<>();
    private final List<List<String>> data = new ArrayList<>();

    private final List<String> currentPrimaryKeys = new ArrayList<>();
    private final List<String> currentForeignKeys = new ArrayList<>();
    private final HashMap<String, String> currentForeignKeysMap = new HashMap<>();
    private String currentTable;


    public DBModel(String accessFile) {
        this.accessFile = accessFile;

        try {
          init();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void init() throws SQLException {
        connection = DriverManager.getConnection(createURL(accessFile));
        statement = connection.createStatement();

        setTableNames();
    }
    public void close(){
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    public void reloadFromAccessFile(String accessFile) {
        close();

        this.accessFile = accessFile;
        try {
            init();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void runQuery(String query) {
        try {
            ResultSet rs = createResultSetFromQuery(query);
            setTableNames();
            setCurrentTable(rs);
            setColumnNames(rs);
            setData(rs);

            setCurrentPrimaryKeys();
            setCurrentForeignKeys();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    private String createURL(String file) {
        if(file.startsWith("C:") || file.startsWith("D:") || file.startsWith("E:")) {
            return String.format("%s%s", PROTOCOL, file);
        }
        return String.format("%s%s", PROTOCOL, ClassLoader.getSystemResource(file).getFile());
    }
    private ResultSet createResultSetFromQuery(String query) throws SQLException {
        return statement.executeQuery(query);
    }

    public String getCurrentTable() {
        return currentTable;
    }
    private void setCurrentTable(ResultSet rs){
        try {
            currentTable = rs.getMetaData().getTableName(1);

        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            currentTable = "No Table";
        }
    }

    public List<String> getColumnNames() {
        return List.copyOf(columnNames);
    }
    private void setColumnNames(ResultSet rs) {
        columnNames.clear();
        try {
            ResultSetMetaData metaData = rs.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columnNames.add(metaData.getColumnName(i));
            }

        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            columnNames.clear();
            columnNames.add("No Columns");
        }
    }

    public List<List<String>> getData() {
        return List.copyOf(data);
    }
    private void setData(ResultSet rs) {

        data.clear();

        try{
        while (rs.next()) {
            List<String> row = new ArrayList<>();

            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                row.add(rs.getString(i));
            }

            data.add(row);
        }
    } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            data.clear();
            data.add(new ArrayList<>(List.of("No data")));
        }
    }

    public List<String> getTableNames() {
        return tableNames;
    }
    private void setTableNames() {
        tableNames.clear();
        try {
            ResultSet catalogs = connection.getMetaData().getTables(null, null, "%", null);
            while(catalogs.next()){
                tableNames.add(catalogs.getString(3));
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            tableNames.clear();
            tableNames.add("No Tables");
        }
    }

    public List<String> getPrimaryKeys() {
        return List.copyOf(currentPrimaryKeys);
    }
    private void setCurrentPrimaryKeys() throws SQLException {
        currentPrimaryKeys.clear();

        ResultSet rs = connection.getMetaData().getPrimaryKeys(null, null, currentTable);
        while(rs.next()){
            currentPrimaryKeys.add(rs.getString("COLUMN_NAME"));
        }
    }
    private void setCurrentForeignKeys() throws SQLException {
        currentForeignKeys.clear();

        ResultSet rs = connection.getMetaData().getImportedKeys(null, null, currentTable);

        while(rs.next()){
            currentForeignKeys.add(rs.getString("FKCOLUMN_NAME"));

            String fkTableName = rs.getString("FKTABLE_NAME");
            String fkColumnName = rs.getString("FKCOLUMN_NAME");
            String pkTableName = rs.getString("PKTABLE_NAME");
            String pkColumnName = rs.getString("PKCOLUMN_NAME");

            currentForeignKeysMap.put("%s.%s".formatted(fkTableName, fkColumnName), "%s.%s".formatted(pkTableName, pkColumnName));
        }
    }

    public List<String> getForeignKeys() {
        return List.copyOf(currentForeignKeys);
    }
    public HashMap<String, String> getForeignKeysMap() {
        return currentForeignKeysMap;
    }
}
