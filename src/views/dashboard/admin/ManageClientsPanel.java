package views.dashboard.admin;

import dao.AppointmentDAO;
import dao.UserDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.ClientRow;
import utils.Theme;

//Panel for Admin/Staff to manage clinic clients.
public class ManageClientsPanel extends JPanel {

    private final UserDAO dao = new UserDAO();
    private final AppointmentDAO apptDAO = new AppointmentDAO();

    //UI Components
    private final JPanel gridPanel;
    private final JTextArea recordsArea = new JTextArea();

    /*Constructor that sets up the UI layout, including the toolbar, the card grid, 
      the search panel on the right, and the records panel at the bottom.*/
      public ManageClientsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Top Toolbar (Buttons)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton removeBtn = actionBtn("Remove Client", Theme.BTN_RED);
        JButton searchBtn = actionBtn("Search Client", Theme.BTN_BLUE);
        JButton allBtn    = actionBtn("Display All", Theme.BTN_GRAY);
        
        toolbar.add(removeBtn);
        toolbar.add(searchBtn);
        toolbar.add(allBtn);

        //Center: Grid of Cards + Right Search Card
        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);

        //Left side: Scrollable Card Grid
        gridPanel = new JPanel(new GridLayout(0, 3, 15, 15)); //3 columns for clients
        gridPanel.setBackground(Theme.CONTENT_BG);

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(Theme.CONTENT_BG);
        gridWrapper.add(gridPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(gridWrapper);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CONTENT_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        middle.add(scroll, BorderLayout.CENTER);
        middle.add(buildSearchCard(), BorderLayout.EAST);

        //Assemble Main Layout
        JPanel topArea = new JPanel(new BorderLayout(0, 14));
        topArea.setOpaque(false);
        topArea.add(toolbar, BorderLayout.NORTH);
        topArea.add(middle, BorderLayout.CENTER);

        add(topArea, BorderLayout.CENTER);
        add(buildRecordsCard(), BorderLayout.SOUTH);

        //Listeners
        removeBtn.addActionListener(e -> onRemovePrompt());
        searchBtn.addActionListener(e -> onSearch());
        allBtn.addActionListener(e -> refresh());

        refresh();
    }

    //Data Operations
   //Public method called by the AdminDashboardFrame to reload all data.
    public void refresh() {
        try {
            populate(dao.getAllClients());
        } catch (SQLException ex) {
            showError("Could not load clients", ex);
        }
    }

    //Make a database search and updates the UI with the filtered results.
    //"keyword" The search term to filter by name, phone, or email.
    private void doSearch(String keyword) {
        try {
            populate(dao.searchClients(keyword));
        } catch (SQLException ex) {
            showError("Search failed", ex);
        }
    }

    //Renders the list of clients into the visual grid and updates the bottom text summary.
    //Dynamically generates display IDs (C001 for registered, W001 for walk-ins).
    private void populate(List<ClientRow> rows) {
        gridPanel.removeAll();
        StringBuilder sb = new StringBuilder();

        int walkInCounter = 0;

        for (ClientRow c : rows) {
            String displayId;
            //Generate the visual ID
            if (c.getType() == ClientRow.Type.REGISTERED) {
                displayId = String.format("C%03d", c.getUserId());
            } else {
                walkInCounter++;
                displayId = String.format("W%03d", walkInCounter);
            }

            ClientCard card = new ClientCard(c, displayId);
            gridPanel.add(card);

            //Build the text summary for the bottom panel
            String emailDisplay = (c.getType() == ClientRow.Type.WALK_IN) ? "(Walk-in)" : (c.getEmail() == null ? "" : c.getEmail());
            String phoneDisplay = (c.getPhone() == null) ? "" : c.getPhone();

            sb.append("[").append(displayId).append(" - ")
              .append(c.getName()).append(", ")
              .append(phoneDisplay.isEmpty() ? "no contact" : phoneDisplay).append(", ")
              .append(emailDisplay).append(", ")
              .append(c.getStatus()).append("]  ");
        }

        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    //Actions
    //Routes the delete request based on the client type.
    //Registered clients are deleted by ID, while walk-ins are deleted by name.
    private void onDelete(ClientRow cr) {
        if (cr.getType() == ClientRow.Type.WALK_IN) {
            onDeleteWalkIn(cr.getName());
        } else {
            int c = JOptionPane.showConfirmDialog(this,
                    "Delete client \"" + cr.getName() + "\"?\nThis removes the account from the system.",
                    "Confirm delete", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            try {
                dao.deleteById(cr.getUserId());
                refresh();
            } catch (SQLException ex) {
                showError("Delete failed", ex);
            }
        }
    }

    //Deletes a walk-in client. 
    /*Since walk-ins don't have a `users` account, deleting them means deleting 
      all their associated appointments from the database.*/
    private void onDeleteWalkIn(String walkInName) {
        try {
            int count = apptDAO.countByWalkInName(walkInName);
            if (count == 0) {
                JOptionPane.showMessageDialog(this, "No appointments found for this walk-in.");
                return;
            }

            String message =
                    "Delete walk-in client \"" + walkInName + "\"?\n\n" +
                    "This will permanently remove " + count +
                    (count == 1 ? " appointment" : " appointments") +
                    " associated with this walk-in.\n" +
                    "Registered clients are not affected.\n\n" +
                    "Continue?";

            int c = JOptionPane.showConfirmDialog(this, message,
                    "Confirm walk-in delete", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;

            int deleted = apptDAO.deleteByWalkInName(walkInName);
            if (deleted > 0) {
                JOptionPane.showMessageDialog(this,
                        deleted + (deleted == 1 ? " appointment" : " appointments") + " deleted.");
            }
            refresh();
        } catch (SQLException ex) {
            showError("Delete walk-in failed", ex);
        }
    }

    //Prompts the user to type an ID or Name to delete.
    //Handles three formats:
    //1. Starts with "W" (Walk-in ID, e.g., W001)
    //2. Starts with "C" (Client ID, e.g., C001)
    //3. Exact Name match
    private void onRemovePrompt() {
        String input = JOptionPane.showInputDialog(this,
                "Enter Client ID to remove:\n\n" +
                "  • Registered client → C001, C002, ...\n" +
                "  • Walk-in client    → W001, W002, ...\n" +
                "  • Or type the name directly");
        if (input == null) return;
        String q = input.trim();
        if (q.isEmpty()) return;

        String upper = q.toUpperCase();

        //Walk-in ID 
        if (upper.startsWith("W")) {
            int target;
            try { target = Integer.parseInt(upper.replaceAll("[^0-9]", "")); } 
            catch (NumberFormatException ex) { return; }
            
            // Search the current grid for the nth walk-in
            int counter = 0;
            for (Component comp : gridPanel.getComponents()) {
                if (comp instanceof ClientCard) {
                    ClientCard card = (ClientCard) comp;
                    if (card.clientRow.getType() == ClientRow.Type.WALK_IN) {
                        counter++;
                        if (counter == target) {
                            onDeleteWalkIn(card.clientRow.getName());
                            return;
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Walk-in not found.");
            return;
        }

        //Registered Client ID 
        if (upper.startsWith("C")) {
            int userId;
            try { userId = Integer.parseInt(upper.replaceAll("[^0-9]", "")); } 
            catch (NumberFormatException ex) { return; }
            
            for (Component comp : gridPanel.getComponents()) {
                if (comp instanceof ClientCard) {
                    ClientCard card = (ClientCard) comp;
                    if (card.clientRow.getUserId() != null && card.clientRow.getUserId() == userId) {
                        onDelete(card.clientRow);
                        return;
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Client not found.");
            return;
        }

        //Match by exact name 
        for (Component comp : gridPanel.getComponents()) {
            if (comp instanceof ClientCard) {
                ClientCard card = (ClientCard) comp;
                if (card.clientRow.getName() != null && card.clientRow.getName().equalsIgnoreCase(q)) {
                    onDelete(card.clientRow);
                    return;
                }
            }
        }
        JOptionPane.showMessageDialog(this, "Client not found.");
    }

    //Prompts the user for a search keyword.
    private void onSearch() {
        String q = JOptionPane.showInputDialog(this, "Search by name, phone, or email:");
        if (q == null) return;
        if (q.trim().isEmpty()) { refresh(); return; }
        doSearch(q.trim());
    }

    //UI Helpers
    //Builds the right-side panel containing the search bar and help instructions.
    private JPanel buildSearchCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(320, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Search Client");
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        
        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        
        JButton searchBtn = actionBtn("Search", Theme.BTN_BLUE);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(searchBtn, BorderLayout.EAST);
        form.add(searchRow);
        form.add(Box.createRigidArea(new Dimension(0, 18)));

        //Help text
        JLabel help1 = new JLabel("<html>Use the search bar above to quickly find a " +
                "specific client by their name, ID, contact number, or email address.</html>");
        help1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        help1.setForeground(Theme.TEXT_MUTED);
        help1.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(help1);
        form.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel help2 = new JLabel("<html>Click the <b>\"Display All\"</b> button on the left " +
                "to reset the table and view the complete list of clients.</html>");
        help2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        help2.setForeground(Theme.TEXT_MUTED);
        help2.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(help2);

        //Search button listener
        searchBtn.addActionListener(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty() || q.startsWith("Enter ")) { refresh(); return; }
            doSearch(q);
        });

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    //Builds the bottom panel that displays a text summary of the clients
    private JPanel buildRecordsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(0, 90));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel header = new JLabel("Client Records");
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

    //Creates a standardized action button.
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
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + ": " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    //Client Card
    //Custom UI component that renders a single client as a visual card.
    //Displays the ID, Status, Name, Phone, and Email
    private class ClientCard extends JPanel {
        public final ClientRow clientRow;

        public ClientCard(ClientRow c, String displayId) {
            this.clientRow = c;
            
            setLayout(new BorderLayout(0, 8));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            setPreferredSize(new Dimension(200, 160));

            //Top: ID & Status 
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            
            JLabel idLbl = new JLabel(displayId);
            idLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            idLbl.setForeground(Theme.TEXT_MUTED);
            
            JLabel statusLbl = new JLabel(c.getStatus());
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            statusLbl.setForeground("Active".equals(c.getStatus()) ? Theme.BTN_GREEN : Theme.BTN_BLUE);
            
            top.add(idLbl, BorderLayout.WEST);
            top.add(statusLbl, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            //Center: Details 
            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

            JLabel name = new JLabel(c.getName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 14));
            name.setForeground(Theme.TEXT_DARK);

            JLabel phone = new JLabel(c.getPhone() == null ? "No phone" : c.getPhone());
            phone.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            phone.setForeground(Theme.TEXT_DARK);

            String emailStr = (c.getType() == ClientRow.Type.WALK_IN) ? "(Walk-in)" : c.getEmail();
            JLabel email = new JLabel(emailStr == null ? "No email" : emailStr);
            email.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            email.setForeground(Theme.TEXT_MUTED);

            center.add(name);
            center.add(Box.createRigidArea(new Dimension(0, 4)));
            center.add(phone);
            center.add(Box.createRigidArea(new Dimension(0, 2)));
            center.add(email);
            
            add(center, BorderLayout.CENTER);

            //Bottom: Action Button 
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            actions.setOpaque(false);

            JButton delBtn = new JButton("Delete");
            delBtn.setBackground(Theme.BTN_RED);
            delBtn.setForeground(Theme.TEXT_WHITE);
            delBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
            delBtn.setFocusPainted(false);
            delBtn.setBorderPainted(false);
            delBtn.setOpaque(true);
            delBtn.setMargin(new Insets(2, 8, 2, 8));
            delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            delBtn.addActionListener(e -> onDelete(c));

            actions.add(delBtn);
            add(actions, BorderLayout.SOUTH);
        }
    }
}