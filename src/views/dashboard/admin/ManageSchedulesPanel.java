package views.dashboard.admin;

import dao.DentistDAO;
import dao.ScheduleDAO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Dentist;
import models.Schedule;
import utils.Theme;

//Panel for managing the Schedules of Dentists.
//Allows Admin/Staff to define which dentist works on which day, at what time, and their availability status.
public class ManageSchedulesPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final Logger LOGGER = Logger.getLogger(ManageSchedulesPanel.class.getName());
    
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final String[] AVAILABILITY = {"Available", "Fully Booked", "Day Off"};

    private final ScheduleDAO dao = new ScheduleDAO();
    private final DentistDAO dentistDAO = new DentistDAO();

    private final JPanel gridPanel = new JPanel(new GridLayout(0, 2, 15, 15));
    private final JTextArea recordsArea = new JTextArea();

    //Form Fields (Right Side)
    private final JComboBox<DentistItem> dentistBox;
    private final JComboBox<String> dayBox;
    private final JComboBox<String> startBox;
    private final JComboBox<String> endBox;
    private final JComboBox<String> availabilityBox;
    private JButton saveBtn;
    private JButton clearBtn;
    private int editingId = -1;

    //Constructor that builds the entire UI and loads the initial data from the database.
    public ManageSchedulesPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Initialize dropdowns for the form
        dentistBox = new JComboBox<>();
        styleCombo(dentistBox);
        
        dayBox = new JComboBox<>(DAYS);
        styleCombo(dayBox);

        String[] times = buildTimeOptions();
        startBox = new JComboBox<>(times);
        styleCombo(startBox);
        
        endBox = new JComboBox<>(times);
        styleCombo(endBox);

        availabilityBox = new JComboBox<>(AVAILABILITY);
        styleCombo(availabilityBox);

        //Build the main layout: Left (Cards), Right (Form), Bottom (Records)
        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);
        middle.add(buildManageCard(), BorderLayout.CENTER);
        middle.add(buildInfoCard(), BorderLayout.EAST);
        
        add(middle, BorderLayout.CENTER);
        add(buildRecordsCard(), BorderLayout.SOUTH);

        //Load data and lock the form initially
        reloadData();
        setFormEnabled(false);
    }

    //UI Building Methods
    //Builds the left-side panel containing the toolbar buttons and the grid of schedule cards.
    private JPanel buildManageCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Manage Schedules");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        //Create the toolbar buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton insertBtn = actionBtn("Insert Schedule", Theme.BTN_GREEN);
        JButton removeBtn = actionBtn("Remove Schedule", Theme.BTN_RED);
        JButton searchBtn = actionBtn("Search Schedule", Theme.BTN_BLUE);
        JButton allBtn = actionBtn("Display All", Theme.BTN_GRAY);
        
        btnRow.add(insertBtn);
        btnRow.add(removeBtn);
        btnRow.add(searchBtn);
        btnRow.add(allBtn);
        body.add(btnRow, BorderLayout.NORTH);

        //Set up the scrollable grid for the cards
        gridPanel.setBackground(Theme.CARD_BG);

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(Theme.CARD_BG);
        gridWrapper.add(gridPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(gridWrapper);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CARD_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(scroll, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);

        //Button Actions
        insertBtn.addActionListener(e -> {
            editingId = -1;
            setFormEnabled(true);
            clearForm();
        });
        removeBtn.addActionListener(e -> onRemovePrompt());
        searchBtn.addActionListener(e -> onSearch());
        allBtn.addActionListener(e -> refresh());

        return card;
    }

    //Builds the right-side panel containing the insert/edit form.
    private JPanel buildInfoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(320, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Schedule Information (Insert / Edit)");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        //Add fields to the form
        form.add(formLabel("Dentist"));
        form.add(dentistBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Day of Week"));
        form.add(dayBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Start Time"));
        form.add(startBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("End Time"));
        form.add(endBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Availability"));
        form.add(availabilityBox);
        form.add(Box.createRigidArea(new Dimension(0, 18)));

        //Add Save and Clear buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        saveBtn  = actionBtn("Save", Theme.BTN_GREEN);
        clearBtn = actionBtn("Clear", Theme.BTN_LIGHT_BLUE);
        clearBtn.setForeground(Theme.TEXT_DARK);
        btns.add(saveBtn);
        btns.add(clearBtn);
        form.add(btns);

        saveBtn.addActionListener(e -> onSaveFromForm());
        clearBtn.addActionListener(e -> {
            editingId = -1;
            clearForm();
            setFormEnabled(false);
        });
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    //Builds the bottom panel that shows a text summary of all schedules.
    private JPanel buildRecordsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(0, 90));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel header = new JLabel("Schedule Records");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        recordsArea.setEditable(false);
        recordsArea.setOpaque(false);
        recordsArea.setLineWrap(true);
        recordsArea.setWrapStyleWord(true);
        recordsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        recordsArea.setForeground(Theme.TEXT_MUTED);
        card.add(recordsArea, BorderLayout.CENTER);
        return card;
    }

    //Data Operations
    //Public method called by the AdminDashboardFrame to reload data.
    public void refresh() {
        reloadData();
    }

    //Reloads both the dentist dropdown and the schedule cards.
    private void reloadData() {
        refreshDentists(); 
        try {
            populate(dao.getAll());
        } catch (SQLException ex) {
            showError("Could not load schedules", ex);
        }
    }

    //Get all dentists from the database and loads them into the dropdown menu.
    private void refreshDentists() {
        try {
            dentistBox.removeAllItems();
            for (Dentist d : dentistDAO.getAll()) {
                dentistBox.addItem(new DentistItem(d.getId(), d.getName()));
            }
        } catch (SQLException ex) {
            showError("Could not load dentists", ex);
        }
    }

    //Clears the grid and fills it with the provided list of schedules.
    private void populate(List<Schedule> rows) {
        gridPanel.removeAll();
        StringBuilder sb = new StringBuilder();

        for (Schedule s : rows) {
            ScheduleCard card = new ScheduleCard(s);
            gridPanel.add(card);

            //Build the text summary for the bottom panel
            sb.append("[").append(s.getFormattedId()).append(" - ")
              .append(s.getDentistName()).append(", ")
              .append(s.getDayOfWeek()).append(", ")
              .append(s.getFormattedTimeSlot()).append(", ")
              .append(s.getAvailability()).append("]    ");
        }
        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    //CRUD Actions
    //Validates the form and saves a new schedule or updates an existing one.
    private void onSaveFromForm() {
        DentistItem dentist = (DentistItem) dentistBox.getSelectedItem();
        if (dentist == null) {
            JOptionPane.showMessageDialog(this, "Please add a dentist first.");
            return;
        }

        String dayOfWeek = (String) dayBox.getSelectedItem();
        LocalTime start = LocalTime.parse((String) startBox.getSelectedItem(), TIME_FMT);
        LocalTime end   = LocalTime.parse((String) endBox.getSelectedItem(),   TIME_FMT);

        //Prevent saving if end time is before start time
        if (!start.isBefore(end)) {
            JOptionPane.showMessageDialog(this, "End time must be after start time.");
            return;
        }

        //Prevent Duplicate Schedule for the same dentist on the same day
        try {
            int excludeId = (editingId >= 0) ? editingId : 0;
            if (dao.hasConflict(dentist.id, dayOfWeek, excludeId)) {
                JOptionPane.showMessageDialog(this, 
                        "Conflict: " + dentist.name + " already has a schedule on " + dayOfWeek + ".\n" +
                        "Please edit the existing schedule instead of adding a new one.", 
                        "Duplicate Schedule", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            showError("Could not validate schedule", ex);
            return;
        }

        try {
            int scheduleId = (editingId >= 0) ? editingId : 0;
            
            Schedule s = new Schedule(
                    scheduleId,
                    dentist.id,
                    dentist.name,
                    dayOfWeek,
                    start, end,
                    (String) availabilityBox.getSelectedItem()
            );

            //Insert or Update based on the editingId
            if (editingId < 0) {
                dao.insert(s);
                JOptionPane.showMessageDialog(this, "Schedule added successfully.");
            } else {
                dao.update(s);
                JOptionPane.showMessageDialog(this, "Schedule updated successfully.");
            }
            
            editingId = -1;
            clearForm();
            refresh();
            setFormEnabled(false);
        } catch (SQLException ex) {
            showError("Save failed", ex);
        }
    }

    //Deletes a schedule from the database by its ID.
    private void onDelete(int id) {
        int c = JOptionPane.showConfirmDialog(this, "Delete schedule " + String.format("S%03d", id) + "?", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        
        try {
            dao.delete(id);
            refresh();
        } catch (SQLException ex) {
            showError("Delete failed", ex);
        }
    }

    //Prompts the user to type a Schedule ID to delete.
    private void onRemovePrompt() {
        String input = JOptionPane.showInputDialog(this, "Enter Schedule ID to remove (e.g. S003):");
        if (input == null || input.trim().isEmpty()) return;
        
        try {
            //Strip out non-numbers (e.g., "S003" becomes 3)
            int id = Integer.parseInt(input.trim().replaceAll("[^0-9]", ""));
            onDelete(id);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid ID format. Use something like S003.");
        }
    }

    //Prompts the user for a search keyword and filters the cards.
    private void onSearch() {
        String q = JOptionPane.showInputDialog(this, "Search schedule by ID (e.g. S003, 3), day, or availability:");
        if (q == null) return;
        if (q.trim().isEmpty()) { refresh(); return; }

        try {
            List<Schedule> all = dao.getAll();
            List<Schedule> filtered = new ArrayList<>();
            String lower = q.trim().toLowerCase();

            for (Schedule s : all) {
                String rawIdStr = String.valueOf(s.getId());
                String formattedIdStr = s.getFormattedId().toLowerCase();
                boolean matchesId = rawIdStr.contains(lower) || formattedIdStr.contains(lower);

                boolean matchesDay = s.getDayOfWeek() != null
                        && s.getDayOfWeek().toLowerCase().contains(lower);

                boolean matchesAvailability = s.getAvailability() != null
                        && s.getAvailability().toLowerCase().contains(lower);

                if (matchesId || matchesDay || matchesAvailability) {
                    filtered.add(s);
                }
            }

            populate(filtered);
        } catch (SQLException ex) {
            showError("Search failed", ex);
        }
    }

    //Fills the right-side form with a schedule's data so it can be edited.
    //Triggered when a user clicks on a schedule card.
    private void loadScheduleIntoForm(Schedule schedule) {
        editingId = schedule.getId();

        //Select the correct dentist in the dropdown
        for (int i = 0; i < dentistBox.getItemCount(); i++) {
            DentistItem dentist = dentistBox.getItemAt(i);
            if (dentist.id == schedule.getDentistId()) {
                dentistBox.setSelectedIndex(i);
                break;
            }
        }

        dayBox.setSelectedItem(schedule.getDayOfWeek());
        startBox.setSelectedItem(schedule.getStartTime().format(TIME_FMT));
        endBox.setSelectedItem(schedule.getEndTime().format(TIME_FMT));
        availabilityBox.setSelectedItem(schedule.getAvailability());
        setFormEnabled(true);
    }

    //Helper Methods
    //Generates a list of 30-minute time slots from 8:00 AM to 6:00 PM.
    private String[] buildTimeOptions() {
        List<String> times = new ArrayList<>();
        for (int h = 8; h <= 18; h++) {
            int h12 = (h > 12 ? h - 12 : (h == 0 ? 12 : h));
            String ampm = (h >= 12 ? "PM" : "AM");
            times.add(String.format("%02d:00 %s", h12, ampm));
            if (h < 18) times.add(String.format("%02d:30 %s", h12, ampm));
        }
        return times.toArray(String[]::new);
    }

    //Resets all form fields back to their default state.
    private void clearForm() {
        if (dentistBox.getItemCount() > 0) dentistBox.setSelectedIndex(0);
        dayBox.setSelectedIndex(0);
        startBox.setSelectedItem("09:00 AM");
        endBox.setSelectedItem("05:00 PM");
        availabilityBox.setSelectedIndex(0);
    }

    //Locks or unlocks the right-side form fields.
    private void setFormEnabled(boolean enabled) {
        dentistBox.setEnabled(enabled);
        dayBox.setEnabled(enabled);
        startBox.setEnabled(enabled);
        endBox.setEnabled(enabled);
        availabilityBox.setEnabled(enabled);
        if (saveBtn != null) saveBtn.setEnabled(enabled);
        if (clearBtn != null) clearBtn.setEnabled(enabled);
    }

    //Applies standard styling to JComboBox elements.
    private <T> void styleCombo(JComboBox<T> box) {
        box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        box.setBackground(Theme.CARD_BG);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        box.setPreferredSize(new Dimension(0, 34));
    }

    //Creates a standard label for form fields.
    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(Theme.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    //Creates a standard action button.
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

    //Displays an error dialog and logs the stack trace.
    private void showError(String msg, Exception ex) {
        LOGGER.log(Level.SEVERE, msg, ex);
        JOptionPane.showMessageDialog(this, msg + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    //Wrapper class to display Dentist names in the dropdown.
    private static class DentistItem {
        final int id; final String name;
        DentistItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    //Schedule Card
    //Custom UI component representing a single schedule as a clickable card.
    private class ScheduleCard extends JPanel {
        public ScheduleCard(Schedule s) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 200));

            //Schedule ID (S003)
            JLabel idLbl = new JLabel(s.getFormattedId());
            idLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            idLbl.setForeground(Theme.TEXT_MUTED);
            idLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Dentist Name (Dr. Maria Jesus)
            JLabel dentistLbl = new JLabel("Dr. " + s.getDentistName());
            dentistLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            dentistLbl.setForeground(Theme.TEXT_DARK);
            dentistLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Dentist ID (D001)
            JLabel dentistIdLbl = new JLabel(String.format("D%03d", s.getDentistId()));
            dentistIdLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            dentistIdLbl.setForeground(Theme.TEXT_MUTED);
            dentistIdLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Day (Monday)
            JLabel dayLbl = new JLabel(s.getDayOfWeek());
            dayLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            dayLbl.setForeground(Theme.TEXT_DARK);
            dayLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Time label (8:00 AM - 06:00 PM)
            JLabel timeLbl = new JLabel(s.getFormattedTimeSlot());
            timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            timeLbl.setForeground(Theme.TEXT_MUTED);
            timeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Availability (Available)
            JLabel availLbl = new JLabel(s.getAvailability());
            availLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            switch (s.getAvailability()) {
                case "Available" -> availLbl.setForeground(Theme.BTN_GREEN);
                case "Fully Booked" -> availLbl.setForeground(Theme.BTN_ORANGE);
                case "Day Off" -> availLbl.setForeground(Theme.BTN_RED);
                default -> availLbl.setForeground(Theme.TEXT_DARK);
            }
            availLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Assemble the card from top to bottom
            //For each individual box of schedules
            add(idLbl);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(dentistIdLbl); 
            add(Box.createRigidArea(new Dimension(0, 6)));
            add(dayLbl);
            add(Box.createRigidArea(new Dimension(0, 2)));
            add(timeLbl);
            add(Box.createRigidArea(new Dimension(0, 6)));
            add(availLbl);

            //Action Buttons (Edit & Delete)
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            actions.setOpaque(false);
            actions.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton editBtn = miniBtn("Edit", Theme.BTN_BLUE);
            editBtn.addActionListener(e -> loadScheduleIntoForm(s));
            
            JButton delBtn = miniBtn("Delete", Theme.BTN_RED);
            delBtn.addActionListener(e -> onDelete(s.getId()));

            actions.add(editBtn);
            actions.add(delBtn);

            add(Box.createVerticalGlue()); //Pushes the buttons to the bottom
            add(actions);

            //Allow clicking the card itself to edit
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    loadScheduleIntoForm(s);
                }
            });
        }

        //Creates a small button for the card actions.
         private JButton miniBtn(String text, Color bg) {
            JButton b = new JButton(text);
            b.setBackground(bg);
            b.setForeground(Theme.TEXT_WHITE);
            b.setFont(new Font("Segoe UI", Font.BOLD, 10));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setMargin(new Insets(2, 8, 2, 8));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return b;
        }
    }
}