package views.dashboard.client;

import dao.AppointmentDAO;
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
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Appointment;
import utils.Session;
import utils.Theme;

//Panel for Clients to view and manage their own appointment history.
//Displays appointments as visual cards in a grid, allowing the client to reschedule (which resets status to Pending) or delete them.
public class MyAppointmentsPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private final AppointmentDAO dao = new AppointmentDAO();

    //UI Components
    private final JPanel gridPanel;
    private final JTextArea recordsArea = new JTextArea();

    //Constructor that sets up the layout (Header, Center Grid, Bottom Records).
    public MyAppointmentsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Header Title
        JLabel header = new JLabel("My Appointment History");
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(Theme.TEXT_DARK);
        add(header, BorderLayout.NORTH);

        //Center Layout (Grid + Records)
        JPanel middle = new JPanel(new BorderLayout(0, 14));
        middle.setOpaque(false);

        //The Grid for Cards (3 columns)
        gridPanel = new JPanel(new GridLayout(0, 3, 15, 15));
        gridPanel.setBackground(Theme.CONTENT_BG);

        //Wrap the grid so cards align to the top and don't stretch vertically
        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(Theme.CONTENT_BG);
        gridWrapper.add(gridPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(gridWrapper);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CONTENT_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        middle.add(scroll, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        //Bottom Records Card (Text Summary)
        JPanel recordsCard = new JPanel(new BorderLayout(0, 8));
        recordsCard.setBackground(Theme.CARD_BG);
        recordsCard.setPreferredSize(new Dimension(0, 90));
        recordsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel recHeader = new JLabel("Appointment Records");
        recHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        recHeader.setForeground(Theme.TEXT_DARK);
        recordsCard.add(recHeader, BorderLayout.NORTH);

        recordsArea.setEditable(false);
        recordsArea.setOpaque(false);
        recordsArea.setLineWrap(true);
        recordsArea.setWrapStyleWord(true);
        recordsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        recordsArea.setForeground(Theme.TEXT_MUTED);
        recordsCard.add(recordsArea, BorderLayout.CENTER);
        
        add(recordsCard, BorderLayout.SOUTH);

        //Initial Load
        refresh();
    }

    //Data Operations
    //Fetches the logged-in client's appointments from the database and populates the visual grid.
    public void refresh() {
        if (Session.getCurrentUser() == null) return;
        int clientId = Session.getCurrentUser().getId();
        
        try {
            //Get only appointments belonging to this specific client
            List<Appointment> list = dao.getAppointmentsByClient(clientId);
            populateGrid(list);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading appointments: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    //Clears the current grid and fills it with visual AppointmentCards 
    //based on the provided list. Also builds the text summary at the bottom.
    private void populateGrid(List<Appointment> list) {
        gridPanel.removeAll(); // Clear old cards
        StringBuilder sb = new StringBuilder();

        for (Appointment a : list) {
            AppointmentCard card = new AppointmentCard(a);
            gridPanel.add(card);

            //Build the text summary for the bottom panel
            sb.append("[").append(a.getFormattedId()).append(" - ")
              .append(a.getFormattedDateTime()).append(" - ")
              .append(a.getStatus()).append("]    ");
        }
        
        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    //CRUD Actions
    //Opens a popup dialog to reschedule an appointment.
    //When saved, it updates the database and resets the status to "Pending".
    private void onReschedule(int id, Appointment existing) {
        //Build the Date and Time pickers, pre-filled with current data
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(
                Date.from(existing.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant()), 
                null, null, Calendar.DAY_OF_MONTH));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "MM/dd/yyyy"));
        
        String[] times = buildTimeOptions();
        JComboBox<String> timeBox = new JComboBox<>(times);
        timeBox.setSelectedItem(existing.getTime().format(TIME_FMT));

        //Layout the dialog
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel("New Preferred Date:"));
        p.add(dateSpinner);
        p.add(new JLabel("New Preferred Time:"));
        p.add(timeBox);
        p.add(new JLabel("<html><i>*Submitting this will reset the status to Pending.</i></html>"));

        int ok = JOptionPane.showConfirmDialog(this, p, "Reschedule Appointment " + String.format("A%03d", id),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return; //User cancelled

        //Extract and validate the new date/time
        Date d = (Date) dateSpinner.getValue();
        LocalDate newDate = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalTime newTime = LocalTime.parse((String) timeBox.getSelectedItem(), TIME_FMT);

        if (newDate.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Please choose a date that is today or later.");
            return;
        }

        //Save to database
        try {
            boolean success = dao.requestReschedule(id, Session.getCurrentUser().getId(), newDate, newTime);
            if (success) {
                JOptionPane.showMessageDialog(this, "Reschedule requested successfully!\nStatus reset to Pending for Admin review.");
                refresh(); //Reload the grid
            } else {
                JOptionPane.showMessageDialog(this, "Unable to reschedule this appointment.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    //Permanently deletes an appointment from the database.
    private void onDelete(int id) {
        if (Session.getCurrentUser() == null) return;
        
        int c = JOptionPane.showConfirmDialog(this,
                "Delete appointment " + String.format("A%03d", id) + " completely?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = dao.deleteOwnedBy(id, Session.getCurrentUser().getId());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Appointment deleted successfully.");
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Unable to delete appointment.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
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

    //Inner Class: Appointment Card
    //Custom UI component representing a single appointment as a card.
    //Shows the status, dentist, service, and date. 
    //Only shows Reschedule/Delete buttons if the status is Pending or Approved.
    private class AppointmentCard extends JPanel {
        public AppointmentCard(Appointment a) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            
            setPreferredSize(new Dimension(220, 160));

            //Top Panel: ID & Status
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            
            JLabel idLbl = new JLabel(a.getFormattedId());
            idLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            idLbl.setForeground(Theme.TEXT_MUTED);
            
            JLabel statusLbl = new JLabel(a.getStatus());
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            switch (a.getStatus()) {
                case "Approved": statusLbl.setForeground(Theme.BTN_GREEN); break;
                case "Pending": statusLbl.setForeground(Theme.BTN_ORANGE); break;
                case "Cancelled": statusLbl.setForeground(Theme.BTN_RED); break;
                case "Completed": statusLbl.setForeground(Theme.BTN_BLUE); break;
                default: statusLbl.setForeground(Theme.TEXT_DARK);
            }
            
            top.add(idLbl, BorderLayout.WEST);
            top.add(statusLbl, BorderLayout.EAST);

            //Details
            JLabel dentistLbl = new JLabel("Dr. " + a.getDentistName());
            dentistLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            dentistLbl.setForeground(Theme.TEXT_DARK);
            dentistLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel serviceLbl = new JLabel(a.getServiceName() == null ? "No Service" : a.getServiceName());
            serviceLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            serviceLbl.setForeground(Theme.TEXT_MUTED);
            serviceLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel dateLbl = new JLabel(a.getFormattedDateTime());
            dateLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            dateLbl.setForeground(Theme.TEXT_DARK);
            dateLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Action Buttons (Only show if Pending or Approved)
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            actions.setOpaque(false);
            actions.setAlignmentX(Component.CENTER_ALIGNMENT);

            if ("Pending".equals(a.getStatus()) || "Approved".equals(a.getStatus())) {
                JButton rescheduleBtn = miniBtn("Reschedule", Theme.BTN_BLUE);
                rescheduleBtn.addActionListener(e -> onReschedule(a.getId(), a));
                
                JButton deleteBtn = miniBtn("Delete", Theme.BTN_RED);
                deleteBtn.addActionListener(e -> onDelete(a.getId()));

                actions.add(rescheduleBtn);
                actions.add(deleteBtn);
            }

            //Assemble the card vertically
            add(top);
            add(Box.createRigidArea(new Dimension(0, 10)));
            add(dentistLbl);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(serviceLbl);
            add(Box.createRigidArea(new Dimension(0, 10)));
            add(dateLbl);
            add(Box.createVerticalGlue()); //Pushes buttons to the bottom
            add(actions);
        }

        //Helper method to create small buttons for the card actions.
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