package com.emla;

import java.sql.*;

public class Main {

    private static final String PROTOCOL = "jdbc:ucanaccess://";
    private static final String DB_ACCESS_FILE = "DB_Lektion_3-4-5.accdb";

    public static void main(String[] args) {

        try {
            start();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }


    private static void start() throws SQLException {
        Connection con = DriverManager.getConnection(getURL(DB_ACCESS_FILE));
        Statement st = con.createStatement();

        ResultSet rs = st.executeQuery("SELECT * FROM Elev");

        var size = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= size; i++) {
            System.out.println(rs.getMetaData().getColumnName(i));
        }

        while(rs.next()) {
            System.out.println(rs.getString(2));
        }
        con.close();
    }

    private static String getURL(String file){
        return PROTOCOL + ClassLoader.getSystemResource(file).getFile();
    }
}