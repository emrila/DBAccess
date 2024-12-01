package com.emla;


public class Main {

    private static final String DB_ACCESS_FILE = "DB_Lektion_3-4-5.accdb";

     public static void main(String[] args) {
         DBModel dbModel = new DBModel(DB_ACCESS_FILE);
         dbModel.runQuery("SELECT * FROM Deltag");
         new DBController(new App(), dbModel);
     }
}