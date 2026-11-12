package views.dashboard.admin;

import dao.UserDAO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.User;
import utils.Theme;

//Panel for the Administrator to manage Staff accounts.
//This panel is only visible to Administrators.
public class ManageStaffPanel extends JPanel {

    private final UserDAO dao = new UserDAO();
    
    //UI Components
    private final JPanel gridPanel = new JPanel(new GridLayout(0, 2, 15, 15));
    private final JTextArea recordsArea = new JTextArea();

    //Form fields on the right side
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JTextField fullNameField;
    private final JTextField emailField;
    private final JTextField phoneField;

    private JButton saveBtn;
    private JButton clearBtn;

    //Tracks if we are updating an existing staff member (holds their ID)
    //If null, we are inserting a new staff member.
    private Integer editingId = null;

    //Constructor that sets up the layout, initializes the form fields, and loads data.
    public ManageStaffPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Initialize Form Fields
        usernameField = textField("Enter username");
        
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        passwordField.setPreferredSize(new Dimension(0, 34));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        fullNameField = textField("Enter full name");
        emailField = textField("Enter email");
        phoneField = textField("Enter phone number");

        //Main Layout (Left grid, Right form, Bottom records)
        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);
        middle.add(buildManageCard(), BorderLayout.CENTER);
        middle.add(buildInfoCard(),   BorderLayout.EAST);
        
        add(middle, BorderLayout.CENTER);
        add(buildRecordsCard(), BorderLayout.SOUTH);

        //Load Data & Lock Form
        refresh();
        setFormEnabled(false);
    }

    //Left Side: Card Grid & Toolbar
    private JPanel buildManageCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Manage Staff");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        //Toolbar Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton insertBtn = actionBtn("+ Add Staff", Theme.BTN_GREEN);
        JButton searchBtn = actionBtn("\uD83D\uDD0D Search Staff", Theme.BTN_BLUE);
        JButton allBtn    = actionBtn("\u2630 Display All", Theme.BTN_GRAY);
        
        btnRow.add(insertBtn);
        btnRow.add(searchBtn);
        btnRow.add(allBtn);
        body.add(btnRow, BorderLayout.NORTH);

        //Grid for Cards (2 columns)
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
            editingId = null;
            setFormEnabled(true);
            clearForm();
            focusField(usernameField);
        });
        searchBtn.addActionListener(e -> onSearch());
        allBtn.addActionListener(e -> refresh());

        return card;
    }

    //Right Side: Information Form
    private JPanel buildInfoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(300, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Staff Information");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        //Add fields to the form
        form.add(formLabel("Username"));
        form.add(usernameField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));

        form.add(formLabel("Password"));
        form.add(passwordField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));

        form.add(formLabel("Full Name"));
        form.add(fullNameField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));

        form.add(formLabel("Email"));
        form.add(emailField);
        form.add(Box.createRigidArea(new Dimension(0, 10)));

        form.add(formLabel("Phone"));
        form.add(phoneField);
        form.add(Box.createRigidArea(new Dimension(0, 18)));

        //Add Save and Clear buttons
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

    //Bottom Panel
    private JPanel buildRecordsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(0, 90));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel header = new JLabel("Staff Records");
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

    //Data Operations & Actions
    public void refresh() {
        try {
            populate(dao.getAllByRole("Staff"));
        } catch (SQLException ex) {
            showError("Could not load staff", ex);
        }
    }

    private void populate(List<User> rows) {
        gridPanel.removeAll();
        StringBuilder sb = new StringBuilder();

        for (User u : rows) {
            StaffCard card = new StaffCard(u);
            gridPanel.add(card);

            sb.append("[").append(String.format("ST%03d", u.getId())).append(" - ")
              .append(u.getFullName()).append(" (").append(u.getUsername()).append(")")
              .append(", ").append(u.getEmail()).append("]    ");
        }
        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    //VALIDATION AND SAVE LOGIC
    private void onSaveFromForm() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim().toLowerCase(); //Normalize email
        String phone    = phoneField.getText().trim();

        //Required Fields Validation
        if (username.isEmpty() || "Enter username".equals(username)) {
            warn("Please enter a username."); focusField(usernameField); return;
        }
        if (password.isEmpty()) {
            warn("Please enter a password."); focusField(passwordField); return;
        }
        if (fullName.isEmpty() || "Enter full name".equals(fullName)) {
            warn("Please enter the full name."); focusField(fullNameField); return;
        }
        if (email.isEmpty() || "Enter email".equals(email) || !email.contains("@")) {
            warn("Please enter a valid email."); focusField(emailField); return;
        }
        if (phone.isEmpty() || "Enter phone number".equals(phone)) {
            warn("Please enter a phone number."); focusField(phoneField); return;
        }

        //Two staff members are allowed to have the same name.
        try {
            if (editingId == null) {
                //INSERT MODE
                if (dao.usernameExists(username)) {
                    error("Username \"" + username + "\" is already taken by another user.");
                    focusField(usernameField);
                    return;
                }
                if (dao.emailExists(email)) {
                    error("Email \"" + email + "\" is already registered to another user.");
                    focusField(emailField);
                    return;
                }

                User staff = new User(0, username, password, "Staff", fullName, email, phone);
                
                if (dao.register(staff)) {
                    success("Staff account created successfully.");
                } else {
                    error("Failed to create account. Please try again.");
                    return;
                }

            } else {
                //UPDATE MODE
                //We use "Except" to ignore the current staff member's own ID
                if (dao.usernameExistsExcept(username, editingId)) {
                    error("Username \"" + username + "\" is already taken by another user.");
                    focusField(usernameField);
                    return;
                }
                if (dao.emailExistsExcept(email, editingId)) {
                    error("Email \"" + email + "\" is already registered to another user.");
                    focusField(emailField);
                    return;
                }

                User staff = new User(editingId, username, password, "Staff", fullName, email, phone);
                
                if (dao.updateById(staff)) {
                    success("Staff account updated successfully.");
                } else {
                    error("Failed to update account. Please try again.");
                    return;
                }
            }
            
            //Reset UI after successful save
            editingId = null;
            clearForm();
            refresh();
            setFormEnabled(false);

        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            //Catches database-level blocks (if two admins save at the exact same time)
            String errorMsg = ex.getMessage().toLowerCase();
            if (errorMsg.contains("username")) {
                error("That username is already taken.");
            } else if (errorMsg.contains("email")) {
                error("That email is already registered.");
            } else {
                showError("Save failed due to duplicate data", ex);
            }
        } catch (SQLException ex) {
            showError("Save failed", ex);
        }
    }

    //Deletes a staff member from the database.
    private void onDelete(User u) {
        int c = JOptionPane.showConfirmDialog(this,
                "Delete staff account \"" + u.getFullName() + "\"?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (c != JOptionPane.YES_OPTION) return;
        
        try {
            dao.deleteById(u.getId());
            refresh();
        } catch (SQLException ex) {
            showError("Delete failed", ex);
        }
    }

    //Prompts the user for a search keyword and filters the staff grid locally.
    private void onSearch() {
        String q = JOptionPane.showInputDialog(this, "Search staff by ID (e.g. ST001, 1), username, or email:");
        if (q == null) return;
        if (q.trim().isEmpty()) { refresh(); return; }
        
        try {
            List<User> all = dao.getAllByRole("Staff");
            List<User> filtered = new ArrayList<>();
            String lower = q.trim().toLowerCase();
            
            //Filter the list based on ID, Username, or Email
            for (User u : all) {
                //Check Username
                boolean matchesUsername = u.getUsername() != null 
                        && u.getUsername().toLowerCase().contains(lower);
                
                //Check Email
                boolean matchesEmail = u.getEmail() != null 
                        && u.getEmail().toLowerCase().contains(lower);
                
                //Check ID (Allows searching by "1", "01", or "ST001")
                String rawIdStr = String.valueOf(u.getId());
                String formattedIdStr = String.format("ST%03d", u.getId()).toLowerCase();
                boolean matchesId = rawIdStr.contains(lower) || formattedIdStr.contains(lower);

                //If it matches any of the three, add it to the results
                if (matchesUsername || matchesEmail || matchesId) {
                    filtered.add(u);
                }
            }
            populate(filtered);
        } catch (SQLException ex) {
            showError("Search failed", ex);
        }
    }

    //Helpers & Styling
    //Helper method to display a warning message dialog.
    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Missing or invalid input", JOptionPane.WARNING_MESSAGE);
    }
    
    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void success(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    //Resets all form fields back to their default placeholder state.
    private void clearForm() {
        usernameField.setText("Enter username");
        usernameField.setForeground(Theme.TEXT_MUTED);
        passwordField.setText("");
        fullNameField.setText("Enter full name");
        fullNameField.setForeground(Theme.TEXT_MUTED);
        emailField.setText("Enter email");
        emailField.setForeground(Theme.TEXT_MUTED);
        phoneField.setText("Enter phone number");
        phoneField.setForeground(Theme.TEXT_MUTED);
    }

    //Locks or unlocks the right-side form components.
    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        fullNameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
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

    //Creates a label for the form with the specified text.
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

    //Displays an error message dialog with the specified message and exception details.
    private void showError(String msg, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + ": " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    //Inner Class: Staff Card
    //Custom UI component representing a single staff member as a card.
    private class StaffCard extends JPanel {
        //Constructor that builds the card layout and its UI components.
        public StaffCard(User u) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 170));

            JLabel idLbl = new JLabel(String.format("ST%03d", u.getId()));
            idLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            idLbl.setForeground(Theme.TEXT_MUTED);
            idLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel nameLbl = new JLabel(u.getFullName());
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLbl.setForeground(Theme.TEXT_DARK);
            nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel userLbl = new JLabel("@" + u.getUsername());
            userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            userLbl.setForeground(Theme.TEXT_MUTED);
            userLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel emailLbl = new JLabel(u.getEmail());
            emailLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            emailLbl.setForeground(Theme.TEXT_DARK);
            emailLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            actions.setOpaque(false);
            actions.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton editBtn = miniBtn("Edit", Theme.BTN_BLUE);
            editBtn.addActionListener(e -> loadStaffIntoForm(u));
            
            JButton delBtn = miniBtn("Delete", Theme.BTN_RED);
            delBtn.addActionListener(e -> onDelete(u));

            actions.add(editBtn);
            actions.add(delBtn);

            add(idLbl);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(nameLbl);
            add(Box.createRigidArea(new Dimension(0, 2)));
            add(userLbl);
            add(Box.createRigidArea(new Dimension(0, 6)));
            add(emailLbl);
            add(Box.createVerticalGlue());
            add(actions);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    loadStaffIntoForm(u);
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

    //Fills the right-side form with the selected staff member's data for editing.
    private void loadStaffIntoForm(User u) {
        editingId = u.getId();
        
        usernameField.setText(u.getUsername());
        usernameField.setForeground(Theme.TEXT_DARK);
        
        //Intentionally leave blank: Admin must type a new password to save
        passwordField.setText(""); 
        
        fullNameField.setText(u.getFullName());
        fullNameField.setForeground(Theme.TEXT_DARK);
        
        emailField.setText(u.getEmail());
        emailField.setForeground(Theme.TEXT_DARK);
        
        phoneField.setText(u.getPhone());
        phoneField.setForeground(Theme.TEXT_DARK);
        
        setFormEnabled(true);
        focusField(passwordField);
    }
}