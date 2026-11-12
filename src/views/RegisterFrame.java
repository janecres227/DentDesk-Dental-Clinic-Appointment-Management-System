package views;
import dao.UserDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import models.User;
import utils.Theme;

//The registration screen for new Clients.
//Allows users to create an account by providing their details.
public class RegisterFrame extends JFrame {

    //Unique brand color for the left branding panel (matches LoginFrame)
    private static final Color LEFT_PANEL_BG = new Color(0, 75, 135);

    //Constructor that sets up the UI layout, including the header, branding, and registration form.
    public RegisterFrame() {
        setTitle("DentDesk - Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 720); //Height adjusted to fit all fields
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        //Header Panel
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.HEADER_BG); 
        header.setPreferredSize(new Dimension(0, 60));
        
        JLabel title = new JLabel("  DentDesk");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Theme.TEXT_WHITE); 
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        //Main Split Panel (Left and Right)
        JPanel mainPanel = new JPanel(new GridLayout(1, 2));

        //Left Panel (Branding) 
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(LEFT_PANEL_BG);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(Box.createVerticalGlue());

        JPanel leftTitle = new JPanel(new GridLayout(2, 1, 0, -6));
        leftTitle.setOpaque(false);
        
        JLabel titleLine1 = new JLabel("DENTAL APPOINTMENT", SwingConstants.CENTER);
        titleLine1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLine1.setForeground(Theme.TEXT_WHITE); 
        
        JLabel titleLine2 = new JLabel("MANAGEMENT SYSTEM", SwingConstants.CENTER);
        titleLine2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLine2.setForeground(Theme.TEXT_WHITE); 
        
        leftTitle.add(titleLine1);
        leftTitle.add(titleLine2);
        leftTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel leftSubtitle = new JLabel("Efficiently manage your dental appointment");
        leftSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        leftSubtitle.setForeground(Theme.TEXT_WHITE); 
        leftSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftSubtitle.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        leftPanel.add(leftTitle);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(leftSubtitle);
        leftPanel.add(Box.createVerticalGlue());
        mainPanel.add(leftPanel);

        //Right Panel (Registration Form) 
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(Theme.CONTENT_BG); 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 40, 3, 40);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        //Form Title
        JLabel titleLabel = new JLabel("Create your account here.");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(18, 40, 14, 40);
        rightPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(2, 40, 2, 40);

        //Form Fields 
        //Username
        JLabel unLabel = new JLabel("Username");
        unLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 1;
        rightPanel.add(unLabel, gbc);

        JTextField unField = new JTextField();
        unField.setBorder(new RoundedBorder(10));
        unField.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 2;
        rightPanel.add(unField, gbc);

        //First Name
        JLabel fnLabel = new JLabel("First Name");
        fnLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 3;
        rightPanel.add(fnLabel, gbc);

        JTextField fnField = new JTextField();
        fnField.setBorder(new RoundedBorder(10));
        fnField.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 4;
        rightPanel.add(fnField, gbc);

        //Last Name
        JLabel lnLabel = new JLabel("Last Name");
        lnLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 5;
        rightPanel.add(lnLabel, gbc);

        JTextField lnField = new JTextField();
        lnField.setBorder(new RoundedBorder(10));
        lnField.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 6;
        rightPanel.add(lnField, gbc);

        //Email
        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 7;
        rightPanel.add(emailLabel, gbc);

        JTextField emailField = new JTextField();
        emailField.setBorder(new RoundedBorder(10));
        emailField.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 8;
        rightPanel.add(emailField, gbc);

        //Phone
        JLabel phoneLabel = new JLabel("Phone");
        phoneLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 9;
        rightPanel.add(phoneLabel, gbc);

        JTextField phoneField = new JTextField();
        phoneField.setBorder(new RoundedBorder(10));
        phoneField.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 10;
        rightPanel.add(phoneField, gbc);

        //Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 11;
        rightPanel.add(passLabel, gbc);

        JPasswordField passField = new JPasswordField();
        passField.setBorder(new RoundedBorder(10));
        passField.setPreferredSize(new Dimension(250, 40));
        gbc.gridy = 12;
        rightPanel.add(passField, gbc);

        //Sign Up Button
        JButton signUpButton = new RoundedButton("Sign up", 15);
        signUpButton.setBackground(Theme.BTN_BLUE); 
        signUpButton.setForeground(Theme.TEXT_WHITE); 
        signUpButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        signUpButton.setPreferredSize(new Dimension(150, 40));
        gbc.gridy = 13;
        gbc.insets = new Insets(16, 40, 16, 40);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        rightPanel.add(signUpButton, gbc);

        //Action Listeners
        signUpButton.addActionListener(e -> performRegistration(
                unField.getText().trim(),
                fnField.getText().trim(),
                lnField.getText().trim(),
                emailField.getText().trim(),
                phoneField.getText().trim(),
                new String(passField.getPassword())
        ));

        mainPanel.add(rightPanel);
        add(mainPanel, BorderLayout.CENTER);
    }

    //Logic Methods
    //Handles the registration logic when the "Sign up" button is clicked.
    //Validates inputs, checks for duplicates, and inserts the new client into the database.
    private void performRegistration(String username, String firstName, String lastName, 
                                     String email, String phone, String password) {
        
        //Basic Validation
        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty() 
                || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields.",
                    "Missing fields", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address.",
                    "Invalid email", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 6 characters.",
                    "Weak password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        //Database Operations
        try {
            UserDAO dao = new UserDAO();

            //Check if username is already taken
            if (dao.usernameExists(username)) {
                JOptionPane.showMessageDialog(this,
                        "That username is already taken.",
                        "Registration failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            //Check if email is already registered
            if (dao.emailExists(email)) {
                JOptionPane.showMessageDialog(this,
                        "That email is already registered.",
                        "Registration failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Create the new User object (Role is always "Client" for self-registration)
            //ID auto-generated by DB
            User newUser = new User(0, username, password, "Client", firstName + " " + lastName, email, phone);            
                    
           //Insert into database
            if (dao.register(newUser)) {
                JOptionPane.showMessageDialog(this,
                        "Account created successfully!\nPlease log in.",
                        "Welcome to DentDesk", JOptionPane.INFORMATION_MESSAGE);
                
                //Redirect back to Login Frame
                dispose();
                new LoginFrame().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Registration failed. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}