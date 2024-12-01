package com.emla;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DBController {

    private static final String SAVED_QUERIES_DIRECTORY = "SavedQueries/";
    private final App APP;
    private final DBModel DB_MODEL;

    private final List<String> QUERY_HISTORY = new ArrayList<>();

    public DBController(App app, DBModel dbModel) {
       APP = app;
       DB_MODEL = dbModel;
       APP.setController(this);
       updateView();
       APP.setAvailableTables(dbModel.getTableNames());
    }
    public void runQuery(String query) {
        DB_MODEL.runQuery(query);
        QUERY_HISTORY.add(query);
        updateView();
    }
    public void updateView() {
        APP.setTableName(DB_MODEL.getCurrentTable());
        APP.setData(DB_MODEL.getColumnNames(), DB_MODEL.getData());

        List<String> foreignKeysFormatted = DB_MODEL.getForeignKeys().stream().map(key -> "%s -> %s".formatted(key, DB_MODEL.getForeignKeysMap().get("%s.%s".formatted(DB_MODEL.getCurrentTable(), key)))).collect(Collectors.toList());
        APP.setKeys(DB_MODEL.getPrimaryKeys(), foreignKeysFormatted);
    }

    public void saveQueryToFile() throws IOException {
        FileWriter writer = new FileWriter("queryHistory.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(writer);

        for (String q : QUERY_HISTORY) {
            bufferedWriter.write(q);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
        }

        bufferedWriter.close();
        writer.close();
    }
    public void saveQueryToFile(String query, String filename) throws IOException {
        FileWriter writer = new FileWriter(SAVED_QUERIES_DIRECTORY+filename + "_query.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write(query);

        bufferedWriter.close();
        writer.close();
    }
    public void openAccessFile(String path){
        DB_MODEL.reloadFromAccessFile(path);
        APP.setAvailableTables(DB_MODEL.getTableNames());
        updateView();
    }

}
