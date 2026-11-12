package views.dashboard.admin;

import dao.ServiceDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Service;
import utils.Theme;

//Panel for Admin/Staff to manage Dental Services.
public class ManageServicesPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(ManageServicesPanel.class.getName());
    private final ServiceDAO dao = new ServiceDAO();
    
    //UI Components
    private final JPanel gridPanel = new JPanel(new GridLayout(0, 2, 15, 15));
    private final JTextArea recordsArea = new JTextArea();

    //Form fields on the right side
    private final JTextField nameField;
    private final JTextArea descriptionArea = new JTextArea(3, 10);
    private final JTextField priceField;
    
    private JButton saveBtn;
    private JButton clearBtn;
    
    //Used to track if we are editing an existing service vs inserting a new one
    private Integer editingId = null;

    //Constructor that sets up the layout, initializes form fields, and loads data.
    public ManageServicesPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Initialize Form Fields
        nameField = textField("Enter service name");
        priceField = textField("Enter price (e.g. 500.00)");
        
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER), 
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        //Build Layout (Left cards, Right form, Bottom records)
        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);
        middle.add(buildManageCard(), BorderLayout.CENTER); 
        middle.add(buildInfoCard(),   BorderLayout.EAST);   
        
        add(middle, BorderLayout.CENTER);
        add(buildRecordsCard(), BorderLayout.SOUTH);        

        //Load Data & Lock Form
        refreshData();
        setFormEnabled(false);
    }

    //Left Side: Card Grid & Toolbar
    //Builds the left-side panel containing the toolbar buttons and the scrollable card grid.
    private JPanel buildManageCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Manage Services");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        //Toolbar Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton insertBtn = actionBtn("+ Insert Service", Theme.BTN_GREEN);
        JButton removeBtn = actionBtn("\uD83D\uDDD1 Remove Service", Theme.BTN_RED);
        JButton searchBtn = actionBtn("\uD83D\uDD0D Search Service", Theme.BTN_BLUE);
        JButton allBtn    = actionBtn("\u2630 Display All", Theme.BTN_GRAY);
        
        btnRow.add(insertBtn);
        btnRow.add(removeBtn);
        btnRow.add(searchBtn);
        btnRow.add(allBtn);
        body.add(btnRow, BorderLayout.NORTH);

        //Grid for Cards (2 columns since the form takes up the right side)
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
            editingId = null;
            setFormEnabled(true);
            clearForm();
            focusField(nameField);
        });
        removeBtn.addActionListener(e -> onRemovePrompt());
        searchBtn.addActionListener(e -> onSearch());
        allBtn.addActionListener(e -> refresh());

        return card;
    }

    //Right Side: Information Form
    //Builds the right-side panel containing the form for inserting new services.
    private JPanel buildInfoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(300, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Service Information");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(formLabel("Service Name"));
        form.add(nameField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Description"));
        form.add(new JScrollPane(descriptionArea));
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Price"));
        form.add(priceField);
        form.add(Box.createRigidArea(new Dimension(0, 18)));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        saveBtn  = actionBtn("\uD83D\uDCBE Save", Theme.BTN_GREEN);
        clearBtn = actionBtn("\u21BB Clear", Theme.BTN_LIGHT_BLUE);
        clearBtn.setForeground(Theme.TEXT_DARK);
        btns.add(saveBtn);
        btns.add(clearBtn);
        form.add(btns);

        saveBtn.addActionListener(e -> onSaveFromForm());
        clearBtn.addActionListener(e -> {
            editingId = null;
            clearForm();
            setFormEnabled(false);
        });
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    //Bottom: Records
    //Builds the bottom panel that displays a text summary of all services.
    private JPanel buildRecordsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(0, 90));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel header = new JLabel("Service Records");
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
        refreshData();
    }

    //Fetches all services from the database and populates the grid.
    private void refreshData() {
        try {
            populate(dao.getAll());
        } catch (SQLException ex) {
            showError("Could not load services", ex);
        }
    }

    //Renders the list of services into the visual grid and updates the text summary.
    private void populate(List<Service> list) {
        gridPanel.removeAll();
        StringBuilder sb = new StringBuilder();
        
        for (Service s : list) {
            ServiceCard card = new ServiceCard(s);
            gridPanel.add(card);
            
            sb.append("[").append(String.format("S%03d", s.getId())).append(" - ")
              .append(s.getServicesName()).append(" - \u20B1")
              .append(String.format("%.2f", s.getPrice())).append("]    ");
        }
        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    //Validates the form and saves a new service to the database.
    private void onSaveFromForm() {
        String name = nameField.getText().trim();
        if (name.isEmpty() || "Enter service name".equals(name)) {
            JOptionPane.showMessageDialog(this, "Please enter a service name.");
            return;
        }
        
        String desc = descriptionArea.getText().trim();
        double price;
        
        //Validate that the price is a valid number
        try {
            String pText = priceField.getText().trim();
            if (pText.isEmpty() || pText.startsWith("Enter")) {
                JOptionPane.showMessageDialog(this, "Please enter a price.");
                return;
            }
            price = Double.parseDouble(pText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number (e.g. 500.00).");
            return;
        }

        try {
            if (editingId == null) {
                //INSERT MODE 
                //Check for duplicate before adding
                if (dao.serviceNameExists(name)) {
                    JOptionPane.showMessageDialog(this, 
                            "Duplication of Services. Cannot be added.", 
                            "Duplicate Service", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                dao.insert(new Service(0, name, desc, price));
                JOptionPane.showMessageDialog(this, "Service added successfully.");
                
            } else {
                //UPDATE MODE 
                //Check for duplicate before updating (ignoring the current service's ID)
                if (dao.serviceNameExistsExcept(name, editingId)) {
                    JOptionPane.showMessageDialog(this, 
                            "Duplication of Services. Cannot be added.", 
                            "Duplicate Service", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                dao.update(new Service(editingId, name, desc, price));
                JOptionPane.showMessageDialog(this, "Service updated successfully.");
            }
            editingId = null;
            clearForm();
            refresh();
            setFormEnabled(false);
            
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            //Database safety net
            JOptionPane.showMessageDialog(this, 
                    "Duplication of Services. Cannot be added.", 
                    "Duplicate Service", 
                    JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            showError("Save failed", ex);
        }
    }

    //Opens a popup dialog to edit an existing service.
    private void onEdit(int id) {
        try {
            Service existing = dao.getAll().stream()
                    .filter(s -> s.getId() == id).findFirst().orElse(null);
            if (existing == null) return;

            //Create temporary input fields for the popup dialog
            JTextField n = new JTextField(existing.getServicesName());
            JTextArea d  = new JTextArea(existing.getDescription() == null ? "" : existing.getDescription(), 3, 20);
            d.setLineWrap(true); d.setWrapStyleWord(true);
            JTextField p = new JTextField(String.valueOf(existing.getPrice()));

            //Layout the dialog
            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
            panel.add(new JLabel("Service Name")); panel.add(n);
            panel.add(new JLabel("Description"));  panel.add(new JScrollPane(d));
            panel.add(new JLabel("Price"));        panel.add(p);

            int ok = JOptionPane.showConfirmDialog(this, panel, "Edit Service",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;

            //Validate the updated price
            double price;
            try { 
                price = Double.parseDouble(p.getText().trim()); 
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Price must be a valid number.");
                return;
            }
            
            //Check for duplicate before saving the edit
            String newName = n.getText().trim();
            if (dao.serviceNameExistsExcept(newName, existing.getId())) {
                JOptionPane.showMessageDialog(this, 
                        "Duplication of Services. Cannot be added.", 
                        "Duplicate Service", 
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            //Save the changes
            existing.setServicesName(newName);
            existing.setDescription(d.getText().trim());
            existing.setPrice(price);
            dao.update(existing);
            refresh();
            
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            //Database safety net
            JOptionPane.showMessageDialog(this, 
                    "Duplication of Services. Cannot be added.", 
                    "Duplicate Service", 
                    JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            showError("Update failed", ex);
        }
    }

    //Deletes a service from the database by its ID.
    private void onDelete(int id) {
        int c = JOptionPane.showConfirmDialog(this,
                "Delete service " + String.format("S%03d", id) + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        
        try {
            dao.delete(id);
            refresh();
        } catch (SQLException ex) {
            showError("Delete failed", ex);
        }
    }

    //Prompts the user to type a Service ID to delete.
    private void onRemovePrompt() {
        String input = JOptionPane.showInputDialog(this,
                "Enter Service ID to remove (e.g. S003):");
        if (input == null || input.trim().isEmpty()) return;
        
        try {
            //Extract only the numbers from the input (e.g. "S003" -> 3)
            int id = Integer.parseInt(input.trim().replaceAll("[^0-9]", ""));
            onDelete(id);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid ID format. Use something like S003.");
        }
    }

    //Prompts the user for a search keyword and filters the cards.
    private void onSearch() {
        String q = JOptionPane.showInputDialog(this, "Search by service name:");
        if (q == null) return;
        
        if (q.trim().isEmpty()) { refresh(); return; }
        
        try {
            populate(dao.search(q.trim()));
        } catch (SQLException ex) {
            showError("Search failed", ex);
        }
    }

    //Helpers & Styling
    //Resets all form fields back to their default placeholder state.
    private void clearForm() {
        nameField.setText("Enter service name");
        nameField.setForeground(Theme.TEXT_MUTED);
        descriptionArea.setText("");
        priceField.setText("Enter price (e.g. 500.00)");
        priceField.setForeground(Theme.TEXT_MUTED);
    }

    //Locks or unlocks the right-side form components.
    private void setFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        descriptionArea.setEnabled(enabled);
        priceField.setEnabled(enabled);
        if (saveBtn  != null) saveBtn.setEnabled(enabled);
        if (clearBtn != null) clearBtn.setEnabled(enabled);
    }

    //Creates a text field with a simulated placeholder effect.
    private JTextField textField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        f.setPreferredSize(new Dimension(0, 34));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        installPlaceholder(f, placeholder);
        return f;
    }

    //Adds a focus listener to make placeholder text disappear when clicked.
    private void installPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Theme.TEXT_MUTED);
        field.setText(placeholder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Theme.TEXT_DARK);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Theme.TEXT_MUTED);
                    field.setText(placeholder);
                }
            }
        });
    }

    //Sets the keyboard cursor to a specific text field.
    private void focusField(JTextField f) {
        SwingUtilities.invokeLater(() -> { f.requestFocusInWindow(); f.selectAll(); });
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
        JOptionPane.showMessageDialog(this, msg + ": " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    //Service Card
    //Custom UI component representing a single service as a card.
    //Uses a vertical BoxLayout to stack the ID, Name, Price, and Buttons.
    private class ServiceCard extends JPanel {
        public ServiceCard(Service s) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            
            //Set a fixed height so the cards stay uniform
            setPreferredSize(new Dimension(200, 150));

            //Service ID
            JLabel idLbl = new JLabel(String.format("S%03d", s.getId()));
            idLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            idLbl.setForeground(Theme.TEXT_MUTED);
            idLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Service Name
            JLabel nameLbl = new JLabel(s.getServicesName());
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLbl.setForeground(Theme.TEXT_DARK);
            nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Price
            JLabel priceLbl = new JLabel(String.format("\u20B1%,.2f", s.getPrice()));
            priceLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            priceLbl.setForeground(Theme.BTN_GREEN);
            priceLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Action Buttons
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            actions.setOpaque(false);
            actions.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton editBtn = miniBtn("Edit", Theme.BTN_BLUE);
            editBtn.addActionListener(e -> onEdit(s.getId()));
            
            JButton delBtn = miniBtn("Delete", Theme.BTN_RED);
            delBtn.addActionListener(e -> onDelete(s.getId()));

            actions.add(editBtn);
            actions.add(delBtn);

            //Assemble vertically
            add(idLbl);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(nameLbl);
            add(Box.createRigidArea(new Dimension(0, 6)));
            add(priceLbl);
            add(Box.createVerticalGlue()); //Pushes the buttons to the bottom
            add(actions);
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