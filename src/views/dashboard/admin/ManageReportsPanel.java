package views.dashboard.admin;

import dao.ReportDAO;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import models.ReportRow;
import models.ReportSummary;
import utils.Theme;

//Panel for generating and exporting financial and appointment reports.
//Allows filtering by date, sorting, viewing summary stats, and exporting to Excel/CSV.
public class ManageReportsPanel extends JPanel {

    private final ReportDAO reportDAO = new ReportDAO();

    //UI Components for Filters
    private final JSpinner fromDateSpinner;
    private final JSpinner toDateSpinner;
    private final JComboBox<String> sortBox;
    
    //Stat Labels (these display the big numbers)
    private final JLabel totalApptsVal = statValue();
    private final JLabel successApptsVal = statValue();
    private final JLabel cancelledApptsVal = statValue();
    private final JLabel incomeVal = statValue();

    //Table Components
    private final DefaultTableModel model;
    private final JTable table;
    
    //Holds the current data so we can export it to CSV
    private List<ReportRow> currentData;

    //Constructor that sets up the entire UI layout (Filters, Stats, and Table).
    public ManageReportsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Top Section: Filters 
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        filterCard.setBackground(Theme.CARD_BG);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        //Create the date pickers
        fromDateSpinner = createDateSpinner();
        toDateSpinner = createDateSpinner();

        //Create the sorting dropdown
        sortBox = new JComboBox<>(new String[]{"Newest First", "Oldest First", "Highest Price", "Lowest Price"});
        sortBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        //Create buttons
        JButton filterBtn = actionBtn("Apply Filter", Theme.BTN_BLUE);
        JButton exportBtn = actionBtn("Export to Excel (CSV)", Theme.BTN_GREEN);

        //Add everything to the filter bar
        filterCard.add(new JLabel("From:"));
        filterCard.add(fromDateSpinner);
        filterCard.add(new JLabel("To:"));
        filterCard.add(toDateSpinner);
        filterCard.add(new JLabel("Sort By:"));
        filterCard.add(sortBox);
        filterCard.add(filterBtn);
        filterCard.add(Box.createHorizontalStrut(20)); 
        filterCard.add(exportBtn);

        add(filterCard, BorderLayout.NORTH);

        //Middle Section: Stats 
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.add(statCard("Total Appointments", totalApptsVal, Theme.STAT_BLUE_BG));
        statsRow.add(statCard("Successful", successApptsVal, Theme.STAT_GREEN_BG));
        statsRow.add(statCard("Cancelled", cancelledApptsVal, Theme.STAT_PURPLE_BG));
        statsRow.add(statCard("Total Income", incomeVal, Theme.STAT_YELLOW_BG));

        //Bottom Section: Table 
        String[] cols = {"ID", "Date", "Client Name", "Dentist", "Service", "Price", "Status"};
        model = new DefaultTableModel(new Object[0][0], cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        configureTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER));
        scroll.getViewport().setBackground(Theme.CARD_BG);

        //Combine stats and table into the center area
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);
        centerPanel.add(statsRow, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        //Action Listeners
        filterBtn.addActionListener(e -> refresh());
        
        exportBtn.addActionListener(e -> {
            if (currentData == null || currentData.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No data to export. Apply a filter first.");
                return;
            }
            exportToCSV();
        });

        //Load initial data when the panel opens
        refresh();
    }

    //Fetches data from the database based on the selected dates and sort order.
    //Updates the stat cards and the table.
    public void refresh() {
        //Get the dates from the spinners
        LocalDate from = getDateFromSpinner(fromDateSpinner);
        LocalDate to = getDateFromSpinner(toDateSpinner);
        String sortBy = (String) sortBox.getSelectedItem();

        //Prevent invalid date ranges
        if (from.isAfter(to)) {
            JOptionPane.showMessageDialog(this, "'From' date cannot be after 'To' date.");
            return;
        }

        try {
            //Load Stats (Total, Success, Cancelled, Income)
            ReportSummary summary = reportDAO.getSummary(from, to);
            totalApptsVal.setText(String.valueOf(summary.totalAppointments));
            successApptsVal.setText(String.valueOf(summary.successfulAppointments));
            cancelledApptsVal.setText(String.valueOf(summary.cancelledAppointments));
            incomeVal.setText(String.format("\u20B1%,.2f", summary.totalIncome));

            //Load Table Data
            currentData = reportDAO.getReportData(from, to, sortBy);
            populateTable(currentData);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Clears the table and fills it with the new list of ReportRow objects.
    private void populateTable(List<ReportRow> rows) {
        model.setRowCount(0); // Clear old data
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        
        for (ReportRow r : rows) {
            model.addRow(new Object[]{
                    String.format("A%03d", r.getAppointmentId()),
                    r.getDate().format(dtf),
                    r.getClientName(),
                    r.getDentistName(),
                    r.getServiceName(),
                    String.format("\u20B1%,.2f", r.getPrice()),
                    r.getStatus()
            });
        }
    }

    //CSV Export Logic
    //Opens a file chooser and saves the current table data into a .csv file.
    //CSV files can be opened directly in Microsoft Excel.
    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report as CSV");
        fileChooser.setSelectedFile(new File("DentDesk_Report_" + LocalDate.now() + ".csv"));

        //If the user clicks Cancel, stop here
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        //Ensure the file ends with .csv
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (FileWriter writer = new FileWriter(file)) {
            //Write the column headers
            writer.write("Appointment ID,Date,Client Name,Dentist,Service,Price,Status\n");

            //Write the data rows
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (ReportRow r : currentData) {
                //We use .replace("\"", "\"\"") to escape any quotes in the names 
                //so the CSV file doesn't break.
                writer.write(String.format("A%03d,%s,\"%s\",\"%s\",\"%s\",%.2f,%s\n",
                        r.getAppointmentId(),
                        r.getDate().format(dtf),
                        r.getClientName() == null ? "" : r.getClientName().replace("\"", "\"\""),
                        r.getDentistName() == null ? "" : r.getDentistName().replace("\"", "\"\""),
                        r.getServiceName() == null ? "" : r.getServiceName().replace("\"", "\"\""),
                        r.getPrice(),
                        r.getStatus()
                ));
            }

            JOptionPane.showMessageDialog(this, "Report successfully exported to:\n" + file.getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to write file: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //UI Helpers
    //Method to configure the appearance and behavior of the data table.
    private void configureTable() {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setBackground(Theme.CARD_BG);
        table.setSelectionBackground(Theme.STAT_BLUE_BG);
        table.setSelectionForeground(Theme.TEXT_DARK);

        JTableHeader th = table.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(Theme.CARD_BG);
        th.setForeground(Theme.TEXT_MUTED);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER));
        th.setPreferredSize(new Dimension(0, 34));

        //Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
    }

    //Helper method to create a date picker (JSpinner) with a specific format.
    private JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "MM/dd/yyyy"));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinner.setPreferredSize(new Dimension(120, 30));
        return spinner;
    }
    
    //Converts the old java.util.Date from the spinner into a modern LocalDate.
    private LocalDate getDateFromSpinner(JSpinner spinner) {
        Date d = (Date) spinner.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    //Creates a label for the big numbers inside the stat cards.
    private JLabel statValue() {
        JLabel l = new JLabel("0");
        l.setFont(new Font("Segoe UI", Font.BOLD, 24));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    //Builds a visual card to hold a statistic (e.g., "Total Income").
    private JPanel statCard(String title, JLabel valueLbl, Color bg) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setForeground(Theme.TEXT_MUTED);
        text.add(t);
        text.add(valueLbl);
        card.add(text, BorderLayout.CENTER);
        return card;
    }
    
    //Creates a styled button.
    private JButton actionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Theme.TEXT_WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return b;
    }
}