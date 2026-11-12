package views;

import dao.UserDAO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import models.User;
import utils.Session;
import utils.Theme;
import views.dashboard.admin.AdminDashboardFrame;   
import views.dashboard.client.ClientDashboardFrame; 

//The main login screen for DentDesk.
//Admin Dashboard or the Client Dashboard based on their database role.
public class LoginFrame extends JFrame {

    //Custom brand color for the left side panel
    private static final Color LEFT_PANEL_BG = new Color(0, 75, 135);

    //Constructor that builds the entire UI layout for the login screen
    //This includes the Header, the Left Branding Panel, and the Right Login Form
    public LoginFrame() {
        setTitle("DentDesk - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        //Header Panel 
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.HEADER_BG);
        header.setPreferredSize(new Dimension(0, 60));
        
        JLabel title = new JLabel("DentDesk");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Theme.TEXT_WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        //Main Split Panel 
        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        
        //Left Panel (Branding) 
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(LEFT_PANEL_BG);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(Box.createVerticalGlue());

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, -3));
        titlePanel.setOpaque(false);
        JLabel line1 = new JLabel("DENTAL APPOINTMENT", SwingConstants.CENTER);
        JLabel line2 = new JLabel("MANAGEMENT SYSTEM", SwingConstants.CENTER);
        line1.setFont(new Font("Segoe UI", Font.BOLD, 22));
        line1.setForeground(Theme.TEXT_WHITE);
        line2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        line2.setForeground(Theme.TEXT_WHITE);
        titlePanel.add(line1);
        titlePanel.add(line2);
        
        JPanel divider = new JPanel();
        divider.setBackground(Theme.TEXT_WHITE);
        divider.setMaximumSize(new Dimension(200, 1));
        divider.setPreferredSize(new Dimension(200, 1));

        JLabel subtitle = new JLabel("Efficiently manage your dental appointment");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(Theme.TEXT_WHITE);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        leftPanel.add(titlePanel);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        leftPanel.add(divider);
        leftPanel.add(subtitle);
        leftPanel.add(Box.createVerticalGlue());
        mainPanel.add(leftPanel);

        //Right Panel (Login Form) 
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(Theme.CONTENT_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 40, 5, 40);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        JLabel welcomeLabel = new JLabel("Welcome Back!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLabel.setForeground(Theme.BTN_BLUE);
        gbc.gridy = 0;
        rightPanel.add(welcomeLabel, gbc);

        JLabel subLabel = new JLabel("Please login to continue", SwingConstants.CENTER);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 40, 25, 40);
        rightPanel.add(subLabel, gbc);

        //Username Field with Placeholder Effect
        gbc.insets = new Insets(5, 40, 5, 40);
        JTextField usernameField = new JTextField("Username");
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setForeground(Theme.TEXT_MUTED);
        usernameField.setBorder(new RoundedBorder(15));
        usernameField.setBackground(Theme.CARD_BG);
        usernameField.setPreferredSize(new Dimension(250, 55));
        
        usernameField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("Username".equals(usernameField.getText())) {
                    usernameField.setText("");
                    usernameField.setForeground(Theme.TEXT_DARK);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (usernameField.getText().isEmpty()) {
                    usernameField.setText("Username");
                    usernameField.setForeground(Theme.TEXT_MUTED);
                }
            }
        });
        gbc.gridy = 2;
        rightPanel.add(usernameField, gbc);

        //Password Field with Placeholder Effect
        JPasswordField passwordField = new JPasswordField("Password");
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setForeground(Theme.TEXT_MUTED);
        passwordField.setBorder(new RoundedBorder(15));
        passwordField.setBackground(Theme.CARD_BG);
        passwordField.setPreferredSize(new Dimension(250, 55));
        passwordField.setEchoChar((char) 0);
        
        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("Password".equals(new String(passwordField.getPassword()))) {
                    passwordField.setText("");
                    passwordField.setForeground(Theme.TEXT_DARK);
                    passwordField.setEchoChar('\u2022'); //Hide text
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (passwordField.getPassword().length == 0) {
                    passwordField.setText("Password");
                    passwordField.setForeground(Theme.TEXT_MUTED);
                    passwordField.setEchoChar((char) 0); //Show text
                }
            }
        });
        gbc.gridy = 3;
        rightPanel.add(passwordField, gbc);

        //Login Button
        JButton loginButton = new RoundedButton("LOGIN", 15);
        loginButton.setBackground(Theme.BTN_BLUE);
        loginButton.setForeground(Theme.TEXT_WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 4;
        gbc.insets = new Insets(20, 40, 10, 40);
        rightPanel.add(loginButton, gbc);

        //OR Label
        JLabel orLabel = new JLabel("OR", SwingConstants.CENTER);
        orLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 40, 10, 40);
        rightPanel.add(orLabel, gbc);

        //Admin Login Button
        JButton adminButton = new RoundedButton("LOGIN AS ADMIN", 15);
        adminButton.setBackground(Theme.CARD_BG);
        adminButton.setForeground(Theme.BTN_BLUE);
        adminButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        adminButton.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 6;
        rightPanel.add(adminButton, gbc);

        //Footer (Register Link)
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footerPanel.setOpaque(false);
        JLabel noAccountLabel = new JLabel("Don't have an account? ");
        noAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel registerLabel = new JLabel("Register");
        registerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        registerLabel.setForeground(Theme.BTN_BLUE);
        registerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        footerPanel.add(noAccountLabel);
        footerPanel.add(registerLabel);
        
        gbc.gridy = 7;
        gbc.insets = new Insets(15, 40, 20, 40);
        rightPanel.add(footerPanel, gbc);

        //Action Listeners 
        //Standard Login Button
        loginButton.addActionListener(e -> performLogin(
                usernameField.getText().trim(), 
                new String(passwordField.getPassword()), 
                false));

        //Admin Login Button
        adminButton.addActionListener(e -> performLogin(
                usernameField.getText().trim(), 
                new String(passwordField.getPassword()), 
                true));

        //Navigate to Register Form
        registerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                new RegisterFrame().setVisible(true);
            }
        });

        mainPanel.add(rightPanel);
        add(mainPanel, BorderLayout.CENTER);
    }

    //Logic Methods
    //Handles the actual database login and routing.
    private void performLogin(String username, String password, boolean isAdminButton) {
        //Basic Validation (check if fields are empty or still showing placeholder)
        if (username.isEmpty() || "Username".equals(username)) {
            JOptionPane.showMessageDialog(this, "Please enter a username.");
            return;
        }
        if (password.isEmpty() || "Password".equals(password)) {
            JOptionPane.showMessageDialog(this, "Please enter a password.");
            return;
        }

        //Authenticate against the Database
        try {
            User user = new UserDAO().login(username, password);
            
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Set the Session (so the app knows who is logged in)
            Session.setCurrentUser(user);
            dispose(); // Close the login window

            //Route to the correct Dashboard based on the button clicked and the user's Role
            if (isAdminButton) {
                //If they used the "Admin" button, verify they actually have admin privileges
                if (Session.isAdmin()) {
                    new AdminDashboardFrame().setVisible(true); 
                } else {
                    JOptionPane.showMessageDialog(this, "This account is not an administrator.", "Access denied", JOptionPane.ERROR_MESSAGE);
                    new LoginFrame().setVisible(true); //Send them back to login
                }
            } else {
                //Normal login button: Check role and route to appropriate dashboard
                if (Session.isAdmin()) {
                    new AdminDashboardFrame().setVisible(true); 
                } else {
                    new ClientDashboardFrame().setVisible(true); 
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}