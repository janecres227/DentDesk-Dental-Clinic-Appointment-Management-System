package views.dashboard.admin;

import dao.AppointmentDAO;
import dao.DentistDAO;
import dao.ServiceDAO;
import dao.UserDAO;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Appointment;
import models.Dentist;
import models.Service;
import models.User;
import utils.Theme;

//Panel for managing clinic appointments.
//Allows Admin/Staff to create, view, approve, edit, and delete appointments
public class ManageAppointmentsPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final Logger LOGGER = Logger.getLogger(ManageAppointmentsPanel.class.getName());

    private final AppointmentDAO apptDAO = new AppointmentDAO();
    private final DentistDAO dentistDAO = new DentistDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final UserDAO userDAO = new UserDAO();

    //UI Components
    private final JPanel gridPanel = new JPanel(new GridLayout(0, 3, 15, 15)); 
    private final JTextArea recordsArea = new JTextArea();

    //Right-Side Form Components
    private final JTextField patientField = new JTextField();
    private final List<ClientItem> loadedClients = new ArrayList<>();
    private final JComboBox<ServiceItem> serviceBox = new JComboBox<>();
    private final JTextField walkInPhoneField = new JTextField();
    private final JComboBox<DentistItem> dentistBox = new JComboBox<>();
    private final JSpinner dateSpinner;
    private final JComboBox<String> timeBox;
    private final JComboBox<String> statusBox;
    private final JTextArea notesArea = new JTextArea(2, 10);

    private JButton saveBtn;
    private JButton clearBtn;
    private boolean formEnabled = false;

    //Constructor that sets up the layout, initializes the form fields, and loads data.
    public ManageAppointmentsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Setup Form Fields 
        patientField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        patientField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        patientField.setPreferredSize(new Dimension(0, 34));
        patientField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        patientField.setToolTipText("Type the patient's name (Registered or Walk-in)");

        walkInPhoneField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        walkInPhoneField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        walkInPhoneField.setPreferredSize(new Dimension(0, 34));
        walkInPhoneField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        styleCombo(dentistBox);
        styleCombo(serviceBox);

        dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MM/dd/yyyy"));
        dateSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        dateSpinner.setPreferredSize(new Dimension(0, 34));

        timeBox = new JComboBox<>(buildTimeOptions());
        styleCombo(timeBox);

        statusBox = new JComboBox<>(new String[]{"Pending", "Approved", "Cancelled", "Completed"});
        styleCombo(statusBox);

        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        //Main Layout 
        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);
        middle.add(buildManageCard(), BorderLayout.CENTER);
        middle.add(buildInfoCard(),   BorderLayout.EAST); 
        add(middle, BorderLayout.CENTER);
        add(buildRecordsCard(), BorderLayout.SOUTH);

        refreshData();
        setFormEnabled(false);
    }

    //UI Construction
    //Builds the left-side panel containing the toolbar and the grid of cards.
    private JPanel buildManageCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Manage Appointments");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        //Toolbar Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton insertBtn = actionBtn("Insert Appointment", Theme.BTN_GREEN);
        JButton removeBtn = actionBtn("Remove Appointment", Theme.BTN_RED);
        JButton searchBtn = actionBtn("Search Appointment", Theme.BTN_BLUE);
        JButton allBtn = actionBtn("Display All", Theme.BTN_GRAY);
        
        btnRow.add(insertBtn);
        btnRow.add(removeBtn);
        btnRow.add(searchBtn);
        btnRow.add(allBtn);
        body.add(btnRow, BorderLayout.NORTH);

        //Grid for Cards
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

        //Button Listeners
        insertBtn.addActionListener(e -> {
            setFormEnabled(true);
            clearForm();
            focusPatientField();
        });
        removeBtn.addActionListener(e -> onRemovePrompt());
        searchBtn.addActionListener(e -> onSearch());
        allBtn.addActionListener(e -> refreshAppointments());

        return card;
    }

    //Builds the right-side panel containing the insert form.
    private JPanel buildInfoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(320, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Appointment Information (Insert)");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(formLabel("Patient Name"));
        form.add(patientField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Contact Number (walk-in)"));
        form.add(walkInPhoneField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Dentist"));
        form.add(dentistBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Dental Service"));
        form.add(serviceBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Date"));
        form.add(dateSpinner);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Time"));
        form.add(timeBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Status"));
        form.add(statusBox);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Notes (optional)"));
        form.add(new JScrollPane(notesArea));
        form.add(Box.createRigidArea(new Dimension(0, 18)));

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
            clearForm();
            setFormEnabled(false);
        });
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    //Builds the bottom panel that displays a text summary of all appointments.
    private JPanel buildRecordsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(0, 90));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel header = new JLabel("Appointment Records");
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
    //Public method called by the AdminDashboardFrame to reload all data.
    public void refresh() {
        refreshData();
    }

    //Reload method that triggers all individual refresh methods.
    private void refreshData() {
        refreshClients();
        refreshDentists();
        refreshServices();
        refreshAppointments();
    }

    //Get all registered clients from the database and keep them in memory for matching.
    private void refreshClients() {
        try {
            loadedClients.clear();
            for (User u : userDAO.getAllByRole("Client")) {
                loadedClients.add(new ClientItem(u.getId(), u.getFullName(), u.getPhone()));
            }
        } catch (SQLException ex) {
            showError("Could not load clients", ex);
        }
    }

    //Get all dentists from the database and loads them into the dropdown
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

    //Get all dental services from the database and loads them into the dropdown.
    private void refreshServices() {
        try {
            serviceBox.removeAllItems();
            serviceBox.addItem(new ServiceItem(-1, "-- No Service --"));
            for (Service s : serviceDAO.getAll()) {
                serviceBox.addItem(new ServiceItem(s.getId(), s.getServicesName()));
            }
        } catch (SQLException ex) {
            showError("Could not load services", ex);
        }
    }

    //Get all appointments and populates the left-side card grid.
    private void refreshAppointments() {
        try {
            populate(apptDAO.getAll());
        } catch (SQLException ex) {
            showError("Could not load appointments", ex);
        }
    }

    //Renders the list of appointments into the visual grid and updates the text summary.
    private void populate(List<Appointment> list) {
        gridPanel.removeAll();
        StringBuilder sb = new StringBuilder();

        for (Appointment a : list) {
            AppointmentCard card = new AppointmentCard(a);
            gridPanel.add(card);

            sb.append("[").append(a.getFormattedId()).append(" - ")
              .append(a.getClientName() == null ? "?" : a.getClientName()).append(" - ")
              .append(a.getStatus()).append("]    ");
        }

        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    //CRUD Actions
    //Validates and saves a new appointment to the database.
    private void onSaveFromForm() {
        ClientResolution patient = resolvePatient();
        DentistItem dentist = (DentistItem) dentistBox.getSelectedItem();
        ServiceItem service = (ServiceItem) serviceBox.getSelectedItem();
        
        if (patient == null) {
            JOptionPane.showMessageDialog(this, "Please enter a patient name.");
            return;
        }
        if (dentist == null) {
            JOptionPane.showMessageDialog(this, "Please add a dentist first.");
            return;
        }

        String walkInPhone = null;
        if (patient.clientId == null) {
            walkInPhone = walkInPhoneField.getText().trim();
            if (walkInPhone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Contact number is required for walk-in clients.");
                return;
            }
        }

        Date d = (Date) dateSpinner.getValue();
        LocalDate date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        //Prevent past dates
        if (date.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Please choose a date that is today or later.");
            return;
        }

        LocalTime time = LocalTime.parse((String) timeBox.getSelectedItem(), TIME_FMT);
        String status = (String) statusBox.getSelectedItem();
        String notes = notesArea.getText().trim();
        
        Integer serviceId = (service == null || service.id == -1) ? null : service.id;
        String serviceName = (service == null || service.id == -1) ? null : service.name;

        //Double-Booking Validation
        try {
            if (apptDAO.hasConflict(dentist.id, date, time, 0)) {
                JOptionPane.showMessageDialog(this, 
                        "This dentist is already booked at this date and time.\nPlease choose another slot.", 
                        "Time Slot Conflict", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            showError("Could not validate schedule", ex);
            return;
        }

        try {
            apptDAO.insert(new Appointment(0, patient.clientId, patient.displayName,
                    patient.walkInName, walkInPhone, dentist.id, dentist.name,
                    serviceId, serviceName, date, time, status, notes));
            //Message to user on successful save
            JOptionPane.showMessageDialog(this, "Appointment added successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            
            clearForm();
            refreshAppointments();
            setFormEnabled(false);
        } catch (SQLException ex) {
            showError("Save failed", ex);
        }
    }

        //Opens a popup dialog to edit an existing appointment.
    private void onEdit(int id) {
        try {
            Appointment existing = apptDAO.getAll().stream()
                    .filter(a -> a.getId() == id).findFirst().orElse(null);
            if (existing == null) return;

            //Temporary UI elements for the popup dialog
            JTextField editPatientField = new JTextField(existing.getClientName() == null ? "" : existing.getClientName());
            editPatientField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            editPatientField.setPreferredSize(new Dimension(0, 34));

            JTextField editPhone = new JTextField(existing.getWalkInPhone() == null ? "" : existing.getWalkInPhone());
            editPhone.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            editPhone.setPreferredSize(new Dimension(0, 34));

            JComboBox<DentistItem> editDentistBox = new JComboBox<>();
            styleCombo(editDentistBox);
            for (int i = 0; i < dentistBox.getItemCount(); i++) editDentistBox.addItem(dentistBox.getItemAt(i));
            for (int i = 0; i < editDentistBox.getItemCount(); i++) {
                if (editDentistBox.getItemAt(i).id == existing.getDentistId()) { editDentistBox.setSelectedIndex(i); break; }
            }

            JComboBox<ServiceItem> editServiceBox = new JComboBox<>();
            styleCombo(editServiceBox);
            for (int i = 0; i < serviceBox.getItemCount(); i++) editServiceBox.addItem(serviceBox.getItemAt(i));
            for (int i = 0; i < editServiceBox.getItemCount(); i++) {
                if (existing.getServiceId() != null && editServiceBox.getItemAt(i).id == existing.getServiceId()) {
                    editServiceBox.setSelectedIndex(i); break;
                }
            }

            JSpinner editDateSpinner = new JSpinner(new SpinnerDateModel(
                    Date.from(existing.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
                    null, null, Calendar.DAY_OF_MONTH));
            editDateSpinner.setEditor(new JSpinner.DateEditor(editDateSpinner, "MM/dd/yyyy"));
            editDateSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            editDateSpinner.setPreferredSize(new Dimension(0, 34));

            JComboBox<String> editTimeBox = new JComboBox<>(buildTimeOptions());
            styleCombo(editTimeBox);
            editTimeBox.setSelectedItem(existing.getTime().format(TIME_FMT));

            JComboBox<String> editStatusBox = new JComboBox<>(new String[]{"Pending", "Approved", "Cancelled", "Completed"});
            styleCombo(editStatusBox);
            editStatusBox.setSelectedItem(existing.getStatus());

            JTextArea editNotesArea = new JTextArea(3, 20);
            editNotesArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            editNotesArea.setLineWrap(true);
            editNotesArea.setWrapStyleWord(true);
            editNotesArea.setText(existing.getNotes() == null ? "" : existing.getNotes());

            //Layout dialog
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 0; p.add(new JLabel("Patient"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; p.add(editPatientField, gbc);

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 1; p.add(new JLabel("Phone"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; p.add(editPhone, gbc);

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 2; p.add(new JLabel("Dentist"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; p.add(editDentistBox, gbc);

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 3; p.add(new JLabel("Service"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; p.add(editServiceBox, gbc);

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 4; p.add(new JLabel("Date"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; p.add(editDateSpinner, gbc);

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 5; p.add(new JLabel("Time"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; p.add(editTimeBox, gbc);

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 6; p.add(new JLabel("Status"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; p.add(editStatusBox, gbc);

            gbc.gridx = 0; gbc.weightx = 0; gbc.gridy = 7; gbc.anchor = GridBagConstraints.NORTH;
            p.add(new JLabel("Notes"), gbc);
            gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1;
            p.add(new JScrollPane(editNotesArea), gbc);

            int ok = JOptionPane.showConfirmDialog(this, p, "Edit Appointment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;

            //Save edited data
            String typedName = editPatientField.getText().trim();
            Integer clientId = null; String walkInName = null;
            for (ClientItem c : loadedClients) {
                if (c.name != null && c.name.equalsIgnoreCase(typedName)) { clientId = c.id; walkInName = null; break; }
            }
            if (clientId == null) walkInName = typedName;

            DentistItem dn = (DentistItem) editDentistBox.getSelectedItem();
            ServiceItem sv = (ServiceItem) editServiceBox.getSelectedItem();
            Date d2 = (Date) editDateSpinner.getValue();
            
            //Prevent past dates on edit
            LocalDate newDate = d2.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (newDate.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Appointment date cannot be in the past.");
                return;
            }

            LocalTime newTime = LocalTime.parse((String) editTimeBox.getSelectedItem(), TIME_FMT);
            String newStatus = (String) editStatusBox.getSelectedItem();

            //Double-Booking Validation
            //1. Check if the date, time, or dentist actually changed.
            boolean detailsChanged = !newDate.equals(existing.getDate()) 
                                  || !newTime.equals(existing.getTime()) 
                                  || dn.id != existing.getDentistId();

            //2. Check if the new status is "Completed" or "Cancelled"
            boolean isFinalStatus = "Completed".equalsIgnoreCase(newStatus) || "Cancelled".equalsIgnoreCase(newStatus);

            //3. Only validate the time slot if the schedule changed AND it's not a final status.
            if (detailsChanged && !isFinalStatus) {
                try {
                    if (apptDAO.hasConflict(dn.id, newDate, newTime, existing.getId())) {
                        JOptionPane.showMessageDialog(this, 
                                "This dentist is already booked at this date and time.\nPlease choose another slot.", 
                                "Time Slot Conflict", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } catch (SQLException ex) {
                    showError("Could not validate schedule", ex);
                    return;
                }
            }

            existing.setClientId(clientId);
            existing.setWalkInName(walkInName);
            existing.setClientName(typedName);
            String phoneText = editPhone.getText().trim();
            existing.setWalkInPhone(phoneText.isEmpty() ? null : phoneText);
            existing.setDentistId(dn.id);
            existing.setDentistName(dn.name);
            existing.setServiceId(sv == null || sv.id == -1 ? null : sv.id);
            existing.setServiceName(sv == null || sv.id == -1 ? null : sv.name);
            existing.setDate(newDate);
            existing.setTime(newTime);
            existing.setStatus(newStatus);
            existing.setNotes(editNotesArea.getText().trim());

            apptDAO.update(existing);
            refreshAppointments();
            
        } catch (SQLException ex) {
            showError("Update failed", ex);
        }
    }

        //Changes the status of an appointment to "Approved" (with Dentist validation).
    private void onApprove(Appointment a) {
        try {
            //Prevent approving appointments that are already in the past
            if (a.getDate() != null && a.getDate().isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, 
                        "Cannot approve this appointment.\n\nThe appointment date (" + a.getDate() + ") is already in the past.", 
                        "Past Date Error", JOptionPane.ERROR_MESSAGE);
                return; //Stop the process here
            }

            //Fetch the dentist's CURRENT status from the database
            Dentist dentist = dentistDAO.getAll().stream()
                    .filter(d -> d.getId() == a.getDentistId())
                    .findFirst().orElse(null);

            if (dentist == null) {
                JOptionPane.showMessageDialog(this, "Dentist not found in the database.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Prevent approval if the dentist is Inactive
            if ("Inactive".equalsIgnoreCase(dentist.getStatus())) {
                JOptionPane.showMessageDialog(this, 
                        "Cannot approve this appointment.\n\nThe assigned dentist (" + dentist.getName() + ") is currently Inactive.", 
                        "Dentist Inactive", JOptionPane.WARNING_MESSAGE);
                return;
            }

            //If active and not a past date, proceed with approval
            apptDAO.updateStatus(a.getId(), "Approved");
            refreshAppointments();

        } catch (SQLException ex) {
            showError("Approve failed", ex);
        }
    }

    //Permanently deletes an appointment from the database.
    private void onDelete(int id) {
        int c = JOptionPane.showConfirmDialog(this, "Delete appointment " + String.format("A%03d", id) + "?", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            apptDAO.delete(id);
            refreshAppointments();
        } catch (SQLException ex) {
            showError("Delete failed", ex);
        }
    }

    //Prompts the user to enter an Appointment ID to delete.
    private void onRemovePrompt() {
        String input = JOptionPane.showInputDialog(this, "Enter Appointment ID to remove (e.g. A003):");
        if (input == null || input.trim().isEmpty()) return;
        try {
            int id = Integer.parseInt(input.trim().replaceAll("[^0-9]", ""));
            onDelete(id);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid ID format. Use something like A003.");
        }
    }

    //Prompts the user for a keyword and filters the appointments grid.
    private void onSearch() {
        String q = JOptionPane.showInputDialog(this, "Search by patient or dentist name:");
        if (q == null) return;
        if (q.trim().isEmpty()) { refreshAppointments(); return; }
        try {
            populate(apptDAO.search(q.trim()));
        } catch (SQLException ex) {
            showError("Search failed", ex);
        }
    }

    //Helper Methods
    //Determines if the typed patient name matches a registered client or is a walk-in.
    private ClientResolution resolvePatient() {
        String text = patientField.getText().trim();
        if (text.isEmpty()) return null;

        for (ClientItem c : loadedClients) {
            if (c.name != null && c.name.equalsIgnoreCase(text)) {
                return new ClientResolution(c.id, c.name, null);
            }
        }
        //If no match, it's a walk-in
        return new ClientResolution(null, text, text); 
    }

    //Clears all input fields in the form.
    private void clearForm() {
        patientField.setText("");
        walkInPhoneField.setText("");
        walkInPhoneField.setEnabled(true);
        walkInPhoneField.setBackground(Theme.CARD_BG);
        if (dentistBox.getItemCount() > 0) dentistBox.setSelectedIndex(0);
        if (serviceBox.getItemCount() > 0) serviceBox.setSelectedIndex(0);
        dateSpinner.setValue(new Date());
        timeBox.setSelectedIndex(0);
        statusBox.setSelectedIndex(0);
        notesArea.setText("");
    }

    //Locks or unlocks all form fields.
    private void setFormEnabled(boolean enabled) {
        this.formEnabled = enabled;
        patientField.setEnabled(enabled);
        dentistBox.setEnabled(enabled);
        serviceBox.setEnabled(enabled);
        dateSpinner.setEnabled(enabled);
        timeBox.setEnabled(enabled);
        statusBox.setEnabled(enabled);
        notesArea.setEnabled(enabled);
        walkInPhoneField.setEnabled(enabled);
        if (saveBtn != null) saveBtn.setEnabled(enabled);
        if (clearBtn != null) clearBtn.setEnabled(enabled);
    }

    //Sets the cursor focus to the patient name field.
    private void focusPatientField() {
        SwingUtilities.invokeLater(() -> {
            patientField.requestFocusInWindow();
            patientField.selectAll();
        });
    }

    //Generates a list of 30-minute time slots from 8:00 AM to 6:00 PM.
    private String[] buildTimeOptions() {
        List<String> times = new ArrayList<>();
        for (int h = 8; h <= 18; h++) {
            int h12 = (h > 12 ? h - 12 : (h == 0 ? 12 : h));
            String ampm = (h >= 12 ? "PM" : "AM");
            times.add(String.format("%02d:00 %s", h12, ampm));
            if (h < 18) times.add(String.format("%02d:30 %s", h12, ampm));
        }
        return times.toArray(new String[0]);
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

    //Creates a standard action button(Insert, Remove, Search Appointments, & Display All).
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

    //Custom UI component representing a single appointment as a card.
    private class AppointmentCard extends JPanel {
        public AppointmentCard(Appointment a) {
            setLayout(new BorderLayout(0, 8));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            setPreferredSize(new Dimension(220, 180));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            
            JLabel idLbl = new JLabel(a.getFormattedId());
            idLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            idLbl.setForeground(Theme.TEXT_MUTED);
            
            JLabel statusLbl = new JLabel(a.getStatus());
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            switch (a.getStatus()) {
                case "Approved": statusLbl.setForeground(Theme.BTN_GREEN); break;
                case "Pending": statusLbl.setForeground(Theme.BTN_ORANGE); break;
                case "Cancelled": statusLbl.setForeground(Theme.BTN_RED); break;
                default: statusLbl.setForeground(Theme.BTN_BLUE);
            }
            
            top.add(idLbl, BorderLayout.WEST);
            top.add(statusLbl, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

            JLabel name = new JLabel(a.getClientName() == null ? "Walk-in" : a.getClientName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 14));
            name.setForeground(Theme.TEXT_DARK);

            JLabel dentist = new JLabel("Dr. " + a.getDentistName());
            dentist.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            dentist.setForeground(Theme.TEXT_DARK);

            JLabel service = new JLabel(a.getServiceName() == null ? "No Service" : a.getServiceName());
            service.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            service.setForeground(Theme.TEXT_MUTED);

            JLabel date = new JLabel(a.getFormattedDateTime());
            date.setFont(new Font("Segoe UI", Font.BOLD, 12));
            date.setForeground(Theme.TEXT_DARK);

            center.add(name);
            center.add(Box.createRigidArea(new Dimension(0, 4)));
            center.add(dentist);
            center.add(Box.createRigidArea(new Dimension(0, 2)));
            center.add(service);
            center.add(Box.createRigidArea(new Dimension(0, 8)));
            center.add(date);
            
            add(center, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.setOpaque(false);

            if ("Pending".equals(a.getStatus())) {
                JButton appBtn = miniBtn("Approve", Theme.BTN_GREEN);
                //Pass the whole appointment object to allow Dentist validation(Validation to avoid approving an appointment for an Inactive dentist)
                appBtn.addActionListener(e -> onApprove(a));
                actions.add(appBtn);
            } else if ("Approved".equals(a.getStatus())) {
                JButton editBtn = miniBtn("Edit", Theme.BTN_BLUE);
                editBtn.addActionListener(e -> onEdit(a.getId()));
                actions.add(editBtn);
            }

            if (!"Cancelled".equals(a.getStatus()) && !"Completed".equals(a.getStatus())) {
                JButton delBtn = miniBtn("Delete", Theme.BTN_RED);
                delBtn.addActionListener(e -> onDelete(a.getId()));
                actions.add(delBtn);
            }

            add(actions, BorderLayout.SOUTH);
        }

        //Creates a small button for the card actions 
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

    //Wrapper classes for dropdown items
    private static class ClientItem {
        final int id; final String name; final String phone;
        ClientItem(int id, String name, String phone) { this.id = id; this.name = name; this.phone = phone; }
    }
    private static class DentistItem {
        final int id; final String name;
        DentistItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
    private static class ServiceItem {
        final int id; final String name;
        ServiceItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
    private static class ClientResolution {
        final Integer clientId; final String displayName; final String walkInName;
        ClientResolution(Integer clientId, String displayName, String walkInName) {
            this.clientId = clientId; this.displayName = displayName; this.walkInName = walkInName;
        }
    }
}