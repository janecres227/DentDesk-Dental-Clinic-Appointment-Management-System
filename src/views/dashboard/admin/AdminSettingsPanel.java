package views.dashboard.admin;
import dao.UserDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import models.User;
import utils.Session;
import utils.Theme;


//Settings panel for the Administrator.
//Allows the Admin to securely change their own password.
public class AdminSettingsPanel extends JPanel {

    private final UserDAO userDAO = new UserDAO();
    private final JPasswordField currentPassField;
    private final JPasswordField newPassField;
    private final JPasswordField confirmPassField;

    public AdminSettingsPanel() {
        setLayout(new GridBagLayout()); //Centers the card
        setBackground(Theme.CONTENT_BG);

        //Main Card 
        JPanel card = new JPanel();
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(450, 420));

        //Title
        JLabel title = new JLabel("Change Password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Theme.TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        //Subtitle
        JLabel sub = new JLabel("Update your administrator password securely.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sub);
        card.add(Box.createRigidArea(new Dimension(0, 25)));

        //Form Fields
        card.add(formLabel("Current Password (Default)"));
        currentPassField = passField();
        card.add(currentPassField);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        card.add(formLabel("New Password"));
        newPassField = passField();
        card.add(newPassField);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        card.add(formLabel("Confirm New Password"));
        confirmPassField = passField();
        card.add(confirmPassField);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        //Action Button
        JButton updateBtn = new JButton("Update Password");
        updateBtn.setBackground(Theme.BTN_BLUE);
        updateBtn.setForeground(Theme.TEXT_WHITE);
        updateBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        updateBtn.setFocusPainted(false);
        updateBtn.setBorderPainted(false);
        updateBtn.setOpaque(true);
        updateBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        updateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        updateBtn.addActionListener(e -> onUpdatePassword());
        
        card.add(updateBtn);
        add(card);
    }

    //Action Logic
    private void onUpdatePassword() {
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Session expired. Please log in again.");
            return;
        }

        String currentPass = new String(currentPassField.getPassword());
        String newPass = new String(newPassField.getPassword());
        String confirmPass = new String(confirmPassField.getPassword());

        //Basic Validation
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPass.length() < 6) {
            JOptionPane.showMessageDialog(this, "New password must be at least 6 characters.", "Weak Password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        //Verify Current Password against the Database
        try {
            //Re-authenticate to ensure the person typing is actually the admin
            User verified = userDAO.login(currentUser.getUsername(), currentPass);
            if (verified == null) {
                JOptionPane.showMessageDialog(this, "Incorrect current password.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Update Password in Database
            if (userDAO.updatePassword(currentUser.getId(), newPass)) {
                JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update password.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        currentPassField.setText("");
        newPassField.setText("");
        confirmPassField.setText("");
    }

    //UI Helpers
    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(Theme.TEXT_DARK);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return l;
    }

    private JPasswordField passField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setPreferredSize(new Dimension(0, 40));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        return f;
    }

    //Called when the panel is opened to clear old inputs
    public void refresh() {
        clearFields();
    }
}
