package views.dashboard.client;

import dao.AppointmentDAO;
import dao.DentistDAO;
import dao.ServiceDAO;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.*;
import models.Appointment;
import models.Dentist;
import models.Service;
import models.User;
import utils.Session;
import utils.Theme;

//Panel for Clients to book a new appointment or manage their existing ones.
//Features a form to schedule appointments, and buttons to remove or cancel them.
public class BookAppointmentPanel extends JPanel {

    private final DentistDAO dentistDAO = new DentistDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final AppointmentDAO apptDAO = new AppointmentDAO();

    //UI Components
    private final JComboBox<DentistItem> dentistBox = new JComboBox<>();
    private final JComboBox<ServiceItem> serviceBox = new JComboBox<>();
    private final JSpinner dateSpinner;
    private final JSpinner timeSpinner;
    private final JTextArea notesArea = new JTextArea(5, 20);

    private final JButton confirmBtn;
    private final JButton clearFormBtn;

    //Callback to return to the previous screen (Dashboard) when done
    private final Runnable onDone; 

    //Constructor that sets up the layout, form fields, and action listeners.
    //The form is wrapped in a JScrollPane to prevent cut-offs on smaller screens.
    public BookAppointmentPanel(Runnable onDone) {
        this.onDone = onDone;

        setLayout(new BorderLayout());
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        //Main Card Container 
        JPanel card = new JPanel();
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        //Title Label
        JLabel title = new JLabel("Schedule New Appointment");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Theme.TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        //Top Action Buttons (Add, Remove, Cancel) 
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton addBtn = actionBtn("+ Add Appointment", Theme.BTN_GREEN);
        JButton removeBtn = actionBtn("\uD83D\uDDD1 Remove Appointment", Theme.BTN_RED);
        JButton cancelBtn = actionBtn("\u23F1 Cancel Appointment", Theme.BTN_ORANGE);
        
        btnRow.add(addBtn);
        btnRow.add(removeBtn);
        btnRow.add(cancelBtn);
        card.add(btnRow);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        //Separator Line
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.CARD_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        //Form Fields 
        //Dentist Selection
        card.add(sectionLabel("Select Dentist"));
        styleCombo(dentistBox);
        card.add(dentistBox);
        card.add(Box.createRigidArea(new Dimension(0, 18)));

        //Service Selection
        card.add(sectionLabel("Select Service"));
        styleCombo(serviceBox);
        card.add(serviceBox);
        card.add(Box.createRigidArea(new Dimension(0, 18)));

        //Date + Time Side-by-Side
        JPanel dateTimeRow = new JPanel(new GridLayout(1, 2, 20, 0));
        dateTimeRow.setOpaque(false);
        dateTimeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel dateCol = new JPanel();
        dateCol.setOpaque(false);
        dateCol.setLayout(new BoxLayout(dateCol, BoxLayout.Y_AXIS));
        dateCol.add(sectionLabel("Preferred Date"));

        dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MM/dd/yyyy"));
        dateSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        dateSpinner.setPreferredSize(new Dimension(0, 38));
        dateCol.add(dateSpinner);
        
        JPanel timeCol = new JPanel();
        timeCol.setOpaque(false);
        timeCol.setLayout(new BoxLayout(timeCol, BoxLayout.Y_AXIS));
        timeCol.add(sectionLabel("Preferred Time"));

        timeSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "hh:mm a"));
        timeSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        timeSpinner.setPreferredSize(new Dimension(0, 38));
        timeCol.add(timeSpinner);

        dateTimeRow.add(dateCol);
        dateTimeRow.add(timeCol);
        card.add(dateTimeRow);
        card.add(Box.createRigidArea(new Dimension(0, 18)));

        //Additional Notes
        card.add(sectionLabel("Additional Notes (Optional)"));
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesScroll.setBorder(BorderFactory.createEmptyBorder());
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        notesScroll.setPreferredSize(new Dimension(0, 110));
        card.add(notesScroll);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        //Bottom Form Buttons (Confirm / Clear) 
        JPanel formBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        formBtnRow.setOpaque(false);
        formBtnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        confirmBtn   = actionBtn("Confirm Booking", Theme.BTN_BLUE);
        clearFormBtn = actionBtn("Clear", Theme.BTN_GRAY);
        
        formBtnRow.add(confirmBtn);
        formBtnRow.add(clearFormBtn);
        card.add(formBtnRow);

        //Wrap the card in a JScrollPane so buttons don't get cut off on small screens
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.CONTENT_BG);
        wrapper.add(card, BorderLayout.NORTH);

        JScrollPane mainScrollPane = new JScrollPane(wrapper);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainScrollPane.getViewport().setBackground(Theme.CONTENT_BG);
        add(mainScrollPane, BorderLayout.CENTER);

        addBtn.addActionListener(e -> {
            setFormEnabled(true);
            clearForm();
        });
        removeBtn.addActionListener(e -> onRemove());
        cancelBtn.addActionListener(e -> onCancel());

        confirmBtn.addActionListener(e -> onConfirm());
        clearFormBtn.addActionListener(e -> {
            clearForm();
            setFormEnabled(false);
        });

        //Initial load & lock form
        refresh();
        setFormEnabled(false);
    }

    //Data Operations
    //Fetches the latest dentists and services from the database and populates the dropdown menus.
    public void refresh() {
        try {
            dentistBox.removeAllItems();
            dentistBox.addItem(new DentistItem(-1, "-- Choose a Dentist --"));
            for (Dentist d : dentistDAO.getAll()) {
                dentistBox.addItem(new DentistItem(d.getId(), d.getName()));
            }
            dentistBox.setSelectedIndex(0);

            serviceBox.removeAllItems();
            serviceBox.addItem(new ServiceItem(-1, "-- Choose a Service --"));
            for (Service s : serviceDAO.getAll()) {
                serviceBox.addItem(new ServiceItem(s.getId(), s.getServicesName()));
            }
            serviceBox.setSelectedIndex(0);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //CRUD Actions
    //Handles the "Confirm Booking" logic.
    //Validates inputs, checks dates, converts java.util.Date to LocalDate/Time, and inserts the new appointment into the database as "Pending".
    private void onConfirm() {
        DentistItem dentist = (DentistItem) dentistBox.getSelectedItem();
        ServiceItem service = (ServiceItem) serviceBox.getSelectedItem();
        
        //Validation
        if (dentist == null || dentist.id < 0) {
            JOptionPane.showMessageDialog(this, "Please choose a dentist.");
            return;
        }
        if (service == null || service.id < 0) {
            JOptionPane.showMessageDialog(this, "Please choose a service.");
            return;
        }
        
        User current = Session.getCurrentUser();
        if (current == null) {
            JOptionPane.showMessageDialog(this, "Session expired. Please log in again.");
            return;
        }
        
        //Convert the Spinner dates to LocalDate/Time
        Date d = (Date) dateSpinner.getValue();
        LocalDate date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        Date t = (Date) timeSpinner.getValue();
        LocalTime time = t.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                .withSecond(0).withNano(0);
                
        //Prevent booking in the past
        if (date.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Please choose a date that is today or later.");
            return;
        }

        String notes = notesArea.getText().trim();
        //Check for Double Booking before inserting
        try {
            if (apptDAO.hasConflict(dentist.id, date, time, 0)) {
                JOptionPane.showMessageDialog(this, 
                        "Sorry, this dentist is already booked at this date and time.\nPlease choose another slot.", 
                        "Time Slot Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error checking schedule: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Insert into DB
        try {
            Appointment appt = new Appointment(
                    0, current.getId(), current.getFullName(), null, null,
                    dentist.id, dentist.name, service.id, service.name,
                    date, time, "Pending", notes);

            apptDAO.insert(appt);

            JOptionPane.showMessageDialog(this,
                    "Your appointment request has been submitted!\n" +
                    "Status: Pending — the clinic will confirm shortly.",
                    "Booking submitted", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            setFormEnabled(false);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Could not save appointment: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Prompts the client for an Appointment ID and deletes it completely.
    private void onRemove() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        String input = JOptionPane.showInputDialog(this,
                "Enter the Appointment ID to remove (e.g. A007):\n\n" +
                "You can only remove your own appointments.");
        if (input == null || input.trim().isEmpty()) return;

        int id;
        try {
            //Extract numbers only (e.g. "A007" -> 7)
            id = Integer.parseInt(input.trim().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid ID format. Use something like A007.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(this,
                "Delete appointment " + String.format("A%03d", id) + "?\n" +
                "This cannot be undone.", "Confirm delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (c != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = apptDAO.deleteOwnedBy(id, current.getId());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Appointment deleted.");
            } else {
                JOptionPane.showMessageDialog(this, "Appointment not found, or it doesn't belong to you.",
                        "Not found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Prompts the client for an Appointment ID and cancels it.
    //Uses `updateStatusOwnedBy` to mark the record as "Cancelled" without deleting it.
    private void onCancel() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        String input = JOptionPane.showInputDialog(this,
                "Enter the Appointment ID to cancel (e.g. A007):\n\n" +
                "You can only cancel your own appointments.\n" +
                "The record will remain, marked Cancelled.");
        if (input == null || input.trim().isEmpty()) return;

        int id;
        try {
            id = Integer.parseInt(input.trim().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid ID format. Use something like A007.");
            return;
        }

        int c = JOptionPane.showConfirmDialog(this,
                "Cancel appointment " + String.format("A%03d", id) + "?",
                "Confirm cancel", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = apptDAO.updateStatusOwnedBy(id, current.getId(), "Cancelled");
            if (ok) {
                JOptionPane.showMessageDialog(this, "Appointment cancelled.");
            } else {
                JOptionPane.showMessageDialog(this, "Appointment not found, or it doesn't belong to you.",
                        "Not found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Cancel failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Helpers & Styling
    //Resets all form inputs back to their default states.
    private void clearForm() {
        if (dentistBox.getItemCount() > 0) dentistBox.setSelectedIndex(0);
        if (serviceBox.getItemCount() > 0) serviceBox.setSelectedIndex(0);
        dateSpinner.setValue(new Date());
        timeSpinner.setValue(new Date());
        notesArea.setText("");
    }

    //Locks or unlocks the form fields.
    //The form is locked by default until the user clicks "+ Add Appointment".
    private void setFormEnabled(boolean enabled) {
        dentistBox.setEnabled(enabled);
        serviceBox.setEnabled(enabled);
        dateSpinner.setEnabled(enabled);
        timeSpinner.setEnabled(enabled);
        notesArea.setEnabled(enabled);
        if (confirmBtn != null) confirmBtn.setEnabled(enabled);
        if (clearFormBtn != null) clearFormBtn.setEnabled(enabled);
    }

    //Helper method to create a consistent style for section titles.
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text); 
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(Theme.TEXT_DARK); 
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0)); 
        return l;
    }

    //Helper method to apply consistent styling to dropdown menus.
    private <T> void styleCombo(JComboBox<T> box) {
        box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        box.setBackground(Theme.CARD_BG); 
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        box.setPreferredSize(new Dimension(0, 38));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    //Helper method to create a styled button with a specific background color.
    private JButton actionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); 
        b.setForeground(Theme.TEXT_WHITE); 
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true); 
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18)); 
        return b;
    }

    //Inner Classes (Dropdown Wrappers)
    //Wrapper class to display Dentist names properly in the JComboBox while keeping their database ID.
    private static class DentistItem {
        final int id; final String name;
        DentistItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    //Wrapper class to display Service names properly in the JComboBox while keeping their database ID.
    private static class ServiceItem {
        final int id; final String name;
        ServiceItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}