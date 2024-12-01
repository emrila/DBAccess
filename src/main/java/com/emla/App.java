package com.emla;

import com.formdev.flatlaf.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {

    private final JFrame FRAME = new JFrame();
    private final JPanel CONTENT_PANE = new JPanel(new BorderLayout());

    private final JPanel DATA_PANEL = new JPanel(new BorderLayout());
    private final JLabel TABLE_NAME = new JLabel();
    private final JTable DATA_TABLE = new JTable();

    private final JPanel QUERY_PANEL = new JPanel(new BorderLayout());
    private final JTextArea QUERY_AREA = new JTextArea();
    private final JButton RUN_QUERY = new JButton("Run Query");

    private final JPanel DATA_KEYS_PANEL = new JPanel();
    private final JPanel AVAILABLE_TABLES = new JPanel();
    private DBController controller;

    private static final String[] SQL_KEYWORDS = {"SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER", "TABLE", "JOIN", "ON", "INTO", "VALUES", "AND", "OR", "NOT", "NULL", "ORDER", "BY", "GROUP", "HAVING", "AS", "INNER", "OUTER", "LEFT", "RIGHT", "FULL", "UNION", "ALL", "DISTINCT", "COUNT", "SUM", "MAX", "MIN", "AVG", "LIKE", "IN", "BETWEEN", "IS", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "ASC", "DESC", "LIMIT", "OFFSET"};

    public App() {

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Error: " + e.getMessage());
        }
        init();
    }

    private void init() {
        JScrollPane dataScrollPane = new JScrollPane(DATA_TABLE);
        dataScrollPane.setPreferredSize(new Dimension(400, 250));
        DATA_PANEL.add(TABLE_NAME, BorderLayout.NORTH);
        DATA_PANEL.add(dataScrollPane, BorderLayout.CENTER);
        DATA_PANEL.add(DATA_KEYS_PANEL, BorderLayout.SOUTH);

        QUERY_AREA.setPreferredSize(new Dimension(400, 100));
        QUERY_AREA.setBorder(BorderFactory.createTitledBorder("Query"));
        QUERY_AREA.setLineWrap(true);
        QUERY_AREA.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    RUN_QUERY.doClick();
                }
            }
        });
        QUERY_AREA.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                highlightSQL();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                highlightSQL();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                highlightSQL();
            }
        });
        RUN_QUERY.addActionListener(_ -> {
            if(controller == null) {
               return;
            }
            if(QUERY_AREA.getText().isBlank()) {
                return;
            }
            controller.runQuery(QUERY_AREA.getText());
        });

        QUERY_PANEL.add(QUERY_AREA, BorderLayout.CENTER);
        QUERY_PANEL.add(RUN_QUERY, BorderLayout.SOUTH);
        QUERY_PANEL.revalidate();
        QUERY_PANEL.repaint();

        JPanel availableTablesContainer = new JPanel();
        availableTablesContainer.add(new JScrollPane(AVAILABLE_TABLES));

        CONTENT_PANE.setPreferredSize(new Dimension(1000, 600));
        CONTENT_PANE.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        CONTENT_PANE.add(new JScrollPane(DATA_PANEL), BorderLayout.CENTER);
        CONTENT_PANE.add(QUERY_PANEL, BorderLayout.SOUTH);
        CONTENT_PANE.add(new JScrollPane(AVAILABLE_TABLES), BorderLayout.NORTH);

        CONTENT_PANE.revalidate();
        CONTENT_PANE.repaint();

        FRAME.setJMenuBar(createMenuBar());

        FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        FRAME.setContentPane(CONTENT_PANE);
        FRAME.pack();
        FRAME.setVisible(true);
    }

    private JMenuBar createMenuBar() {

        JMenuItem openAccessFile = createMenuItem("Open MS Access File", _ -> {
           JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Access Files", "accdb"));
            fileChooser.setDragEnabled(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            int result = fileChooser.showOpenDialog(FRAME);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                System.out.println("Selected file abs path: " + file.getAbsolutePath());
                System.out.println("Selected file name: " + file.getName());
                System.out.println("Selected file path: " + file.getPath());
                controller.openAccessFile(fileChooser.getSelectedFile().getPath());
            }
        });
        JMenuItem saveQuery = createMenuItem("Save Query History", _ -> {
            try {
                controller.saveQueryToFile();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }

        });
        JMenuItem saveCurrentQuery = createMenuItem("Save Current Query", _ -> {
            String query = QUERY_AREA.getText();
            if (query.isBlank()) {
                return;
            }
            String filename = JOptionPane.showInputDialog(FRAME, "Enter filename");
            if (filename == null || filename.isBlank()) {
                return;
            }
            try {
                controller.saveQueryToFile(query, filename);
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        });

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(openAccessFile);
        fileMenu.add(saveQuery);
        fileMenu.add(saveCurrentQuery);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);

        return menuBar;
    }
    private JMenuItem createMenuItem(String text, ActionListener actionListener) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(actionListener);
        return menuItem;
    }
    private JList<String> createList(String title, List<String> items) {
        JList<String> list = new JList<>(items.toArray(new String[0]));
        list.setFixedCellHeight(20);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setBorder(BorderFactory.createTitledBorder(title));
        return list;
    }
    private void highlightSQL() {

        Highlighter highlighter = QUERY_AREA.getHighlighter();
        highlighter.removeAllHighlights();

        String text = QUERY_AREA.getText().toUpperCase();
        for (String keyword : SQL_KEYWORDS) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                try {
                    highlighter.addHighlight(matcher.start(), matcher.end(), new DefaultHighlighter.DefaultHighlightPainter(Color.PINK));

                } catch (BadLocationException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        QUERY_AREA.repaint();
    }
    public void setTableName(String tableName) {
        TABLE_NAME.setText(tableName);
    }
    public void setData(List<String> columnNames, List<List<String>> data) {
        DefaultTableModel model = new DefaultTableModel();
        columnNames.forEach(model::addColumn);

        data.stream().map(List::toArray).forEach(model::addRow);

        DATA_TABLE.setModel(model);
        DATA_TABLE.revalidate();
        DATA_TABLE.repaint();

        CONTENT_PANE.revalidate();
        CONTENT_PANE.repaint();
    }
    public void setKeys(List<String> primaryKeys, List<String> foreignKeys) {
        DATA_KEYS_PANEL.removeAll();

        DATA_KEYS_PANEL.setLayout(new GridLayout(1, 3));
        DATA_KEYS_PANEL.setBorder(BorderFactory.createTitledBorder("Keys"));
        DATA_KEYS_PANEL.add(createList("Primary Keys: ", primaryKeys));
        DATA_KEYS_PANEL.add(createList("Foreign Keys: ", foreignKeys));

        DATA_KEYS_PANEL.revalidate();
        DATA_KEYS_PANEL.repaint();
    }
    public void setAvailableTables(List<String> tableNames) {

        AVAILABLE_TABLES.removeAll();
        AVAILABLE_TABLES.setLayout(new GridLayout(1, tableNames.size()));

        AVAILABLE_TABLES.setBorder(BorderFactory.createTitledBorder("Tables"));
        for (String tableName : tableNames) {
            JButton button = new JButton(tableName);
            if(button.getText().equals(TABLE_NAME.getText())){
                button.grabFocus();
            }
            button.addActionListener(_ -> {
                controller.runQuery("SELECT * FROM " + tableName);
                controller.updateView();
            });

            AVAILABLE_TABLES.add(button);
        }

        AVAILABLE_TABLES.revalidate();
        AVAILABLE_TABLES.repaint();
    }
    public void setController(DBController controller) {
        this.controller = controller;
    }

}