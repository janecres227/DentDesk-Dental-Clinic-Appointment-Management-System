package views.dashboard.admin;

import dao.AppointmentDAO;
import dao.DentistDAO;
import dao.ServiceDAO;
import dao.UserDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Appointment;
import models.ClientRow;
import models.Service;
import utils.Session;
import utils.Theme;
import views.LoginFrame;
import views.components.SidebarPanel; 

//The main dashboard window for the Administrator and Staff.
public class AdminDashboardFrame extends JFrame {

    //Layout manager to switch between different content panels
    private final CardLayout cardLayout = new CardLayout(); 
    private final JPanel contentPanel = new JPanel(cardLayout); 
    
    //Panel References 
    private ManageAppointmentsPanel apptsPanel;
    private ManageServicesPanel servicesPanel;
    private ManageClientsPanel clientsPanel;
    private ManageSchedulesPanel schedulesPanel;
    private ManageStaffPanel staffPanel;
    private ManageReportsPanel reportsPanel;
    private AdminSettingsPanel settingsPanel;

    //DAOs for the Home Dashboard 
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final DentistDAO dentistDAO = new DentistDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final UserDAO userDAO = new UserDAO();

    //UI Components for the Home Dashboard
    private JLabel totalDentistsLbl;
    private JLabel totalClientsLbl;
    private JLabel totalAppointmentsLbl;
    private JLabel todayScheduleLbl;

    private JPanel apptListPanel;
    private JPanel svcListPanel;
    private JPanel clientListPanel;
    private JPanel activityListPanel;

    //Constructor that sets up the main frame and assembles the UI.
    public AdminDashboardFrame() {
        setTitle("DentDesk - Dental Clinic Appointment System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800); //Slightly wider for modern screens
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
    }
    
    //Builds the top header panel.
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.HEADER_BG);
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 0, 0, 30)), 
                BorderFactory.createEmptyBorder(0, 20, 0, 20))
        );

        String roleSuffix = Session.isAdministrator() ? "Administrator" : "Staff";
        JLabel title = new JLabel("DentDesk  -  Dental Clinic Appointment System (" + roleSuffix + ")");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Theme.TEXT_WHITE); 
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        right.setOpaque(false);

        String display = roleSuffix;
        if (Session.getCurrentUser() != null
                && Session.getCurrentUser().getFullName() != null
                && !Session.getCurrentUser().getFullName().isEmpty()) {
            display = Session.getCurrentUser().getFullName() + " (" + roleSuffix + ")";
        }

        JLabel who = new JLabel(display);
        who.setForeground(Theme.TEXT_WHITE); 
        who.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel avatar = new JLabel("\uD83D\uDC64");
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        right.add(who);
        right.add(avatar);
        header.add(right, BorderLayout.EAST);

        return header;
    }
    
    //Creates the sidebar navigation panel.
    private JPanel buildSidebar() {
        SidebarPanel sidebarPanel = new SidebarPanel(this::handleNavigation);

        sidebarPanel.addItem("\uD83C\uDFE0", "Dashboard", "home", true);
        sidebarPanel.addItem("\uD83E\uDDB7", "Manage Dentists", "dentists", false);
        sidebarPanel.addItem("\uD83D\uDCC5", "Manage Appointments", "appts", false);
        sidebarPanel.addItem("\uD83D\uDEE0", "Manage Services", "services", false);
        sidebarPanel.addItem("\uD83D\uDC65", "Clients", "clients", false);
        sidebarPanel.addItem("\uD83D\uDDD3", "Schedules", "scheds", false);

        if (Session.isAdministrator()) {
            sidebarPanel.addItem("\uD83D\uDC77", "Manage Staff", "staff", false);
            sidebarPanel.addItem("\uD83D\uDCCA", "Reports", "reports", false);
            sidebarPanel.addItem("\u2699\uFE0F", "Settings", "settings", false);
        }

        sidebarPanel.addItem("\uD83D\uDEAA", "Logout", "logout", false);
        sidebarPanel.finish(); 
        return sidebarPanel;
    } 
    
    //This method is called by the SidebarPanel whenever a menu item is clicked.
    private void handleNavigation(String key) {
        if ("logout".equals(key)) {
            int c = JOptionPane.showConfirmDialog(this,
                    "Log out of DentDesk?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                Session.clear();
                dispose();
                new LoginFrame().setVisible(true);
            }
            return;
        }

        //Refresh panels when navigating to them
        switch(key) {
            case "home":
                refreshHomeData();
                break;
            case "appts":
                if (apptsPanel != null) apptsPanel.refresh();
                break;
            case "services":
                if (servicesPanel != null) servicesPanel.refresh();
                break;
            case "clients":
                if (clientsPanel != null) clientsPanel.refresh();
                break;
            case "scheds":
                if (schedulesPanel != null) schedulesPanel.refresh();
                break;
            case "staff":
                if (staffPanel != null) staffPanel.refresh();
                break;
            case "reports":
                if (reportsPanel != null) reportsPanel.refresh();
                break;
            case "settings":
                if (settingsPanel != null) settingsPanel.refresh();
                break;
            default:
            
                break;
        }   
        cardLayout.show(contentPanel, key);
        }
    
    //Builds the main content area using CardLayout.
    private JPanel buildContent() {
        contentPanel.setBackground(Theme.CONTENT_BG);

        //Add the integrated Home Panel
        contentPanel.add(buildHomePanel(), "home");

        contentPanel.add(new ManageDentistsPanel(), "dentists");

        apptsPanel = new ManageAppointmentsPanel();
        contentPanel.add(apptsPanel, "appts");

        servicesPanel = new ManageServicesPanel();
        contentPanel.add(servicesPanel, "services");

        clientsPanel = new ManageClientsPanel();
        contentPanel.add(clientsPanel, "clients");

        schedulesPanel = new ManageSchedulesPanel();
        contentPanel.add(schedulesPanel, "scheds");

        if (Session.isAdministrator()) {
            staffPanel = new ManageStaffPanel();
            contentPanel.add(staffPanel, "staff");

            reportsPanel = new ManageReportsPanel();
            contentPanel.add(reportsPanel, "reports");

            settingsPanel = new AdminSettingsPanel();
            contentPanel.add(settingsPanel, "settings");
        }
        
        refreshHomeData(); //Load live data initially
        return contentPanel;
    }

    //HOME PANEL CONSTRUCTION 
    private JPanel buildHomePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 20));
        p.setBackground(Theme.CONTENT_BG);
        p.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        //TOP SECTION: Welcome Header & Stat Cards
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        String adminName = (Session.getCurrentUser() != null) ? Session.getCurrentUser().getFullName() : "Administrator";
        JLabel welcomeLbl = new JLabel("Welcome back, " + adminName + "!");
        welcomeLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLbl.setForeground(new Color(40, 40, 40));
        welcomeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subLbl = new JLabel("Here is the overview of your clinic today.");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLbl.setForeground(new Color(120, 120, 120));
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        headerPanel.add(welcomeLbl);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        headerPanel.add(subLbl);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        totalDentistsLbl = new JLabel("0");
        totalClientsLbl = new JLabel("0");
        totalAppointmentsLbl = new JLabel("0");
        todayScheduleLbl = new JLabel("0");

        cardsPanel.add(createStatCard("Total Dentists", totalDentistsLbl, "\uD83E\uDDB7", new Color(235, 245, 255)));
        cardsPanel.add(createStatCard("Total Clients", totalClientsLbl, "\uD83D\uDC65", new Color(235, 255, 235)));
        cardsPanel.add(createStatCard("Total Appointments", totalAppointmentsLbl, "\uD83D\uDCC5", new Color(245, 235, 255)));
        cardsPanel.add(createStatCard("Today's Schedule", todayScheduleLbl, "\u23F0", new Color(255, 250, 230)));

        topPanel.add(headerPanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        topPanel.add(cardsPanel);
        
        p.add(topPanel, BorderLayout.NORTH);

        //CENTER SECTION: 3 Lists 
        JPanel centerListsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        centerListsPanel.setOpaque(false);

        apptListPanel = createListContentPanel();
        centerListsPanel.add(createListBox("All Appointments", apptListPanel));

        svcListPanel = createListContentPanel();
        centerListsPanel.add(createListBox("All Services", svcListPanel));

        clientListPanel = createListContentPanel();
        centerListsPanel.add(createListBox("Clients (Registered & Walk-in)", clientListPanel));

        p.add(centerListsPanel, BorderLayout.CENTER);

        //BOTTOM SECTION: Recent Activity 
        JPanel activityPanel = createActivityPanel();
        activityPanel.setPreferredSize(new Dimension(0, 140)); 
        p.add(activityPanel, BorderLayout.SOUTH);

        return p;
    }

    //LIVE DATA FETCHING FOR HOME DASHBOARD
     private void refreshHomeData() {
        try {
            //1. Update Stat Cards
            totalDentistsLbl.setText(String.valueOf(dentistDAO.getAll().size()));
            totalClientsLbl.setText(String.valueOf(userDAO.countAllClients()));
            totalAppointmentsLbl.setText(String.valueOf(appointmentDAO.countAll()));
            todayScheduleLbl.setText(String.valueOf(appointmentDAO.countToday()));

            //2. Update Appointments List
            apptListPanel.removeAll();
            List<Appointment> appointments = appointmentDAO.getAll();
            for (Appointment a : appointments) {
                String text = a.getFormattedId() + " - " + a.getClientName() + " (" + a.getDentistName() + ")";
                apptListPanel.add(createListItem(text));
            }

            //3. Update Services List
            svcListPanel.removeAll();
            List<Service> services = serviceDAO.getAll();
            for (Service s : services) {
                String text = s.getServicesName() + " - \u20B1" + String.format("%,.2f", s.getPrice());
                svcListPanel.add(createListItem(text));
            }

            //4. Update Clients List
            clientListPanel.removeAll();
            List<ClientRow> clients = userDAO.getAllClients();
            for (ClientRow c : clients) {
                String typePrefix = (c.getType() == ClientRow.Type.WALK_IN) ? "Walk-in: " : "Reg: ";
                String text = typePrefix + c.getName();
                clientListPanel.add(createListItem(text));
            }

            //5. Update Recent Activity
            activityListPanel.removeAll();
            List<Appointment> recentAppts = appointmentDAO.getRecentAppointments(5);
            
            if (recentAppts.isEmpty()) {
                JLabel empty = new JLabel("No recent activity found.");
                empty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                empty.setForeground(Color.GRAY);
                activityListPanel.add(empty);
            } else {
                for (Appointment a : recentAppts) {
                    String icon = "\uD83D\uDCC5"; //Default calendar
                    String text = "Appt " + a.getFormattedId() + ": " + a.getClientName() + " with " + a.getDentistName();
                    
                    if ("Completed".equalsIgnoreCase(a.getStatus())) {
                        icon = "\u2705"; text += " was completed.";
                    } else if ("Cancelled".equalsIgnoreCase(a.getStatus())) {
                        icon = "\u274C"; text += " was cancelled.";
                    } else if ("Approved".equalsIgnoreCase(a.getStatus())) {
                        icon = "\uD83D\uDC4D"; text += " was approved.";
                    } else {
                        text += " was booked.";
                    }

                    String time = a.getDate() != null ? a.getDate().toString() : "Recently";
                    activityListPanel.add(createActivityRow(icon, text, time));
                    activityListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }

            revalidate();
            repaint();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading dashboard data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //UI HELPER METHODS
    //Recent Activilty Panel and List Creation
    private JPanel createStatCard(String title, JLabel valueLbl, String icon, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(new Color(100, 100, 100));

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLbl.setForeground(new Color(30, 30, 30));

        textPanel.add(titleLbl);
        textPanel.add(Box.createVerticalGlue());
        textPanel.add(valueLbl);

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLbl.setVerticalAlignment(SwingConstants.CENTER);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(iconLbl, BorderLayout.EAST);
        return card;
    }

    private JPanel createListBox(String title, JPanel listContent) {
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(new Color(40, 40, 40));
        box.add(titleLbl, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(listContent);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        box.add(scroll, BorderLayout.CENTER);
        return box;
    }

    private JPanel createListContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private JLabel createListItem(String text) {
        JLabel itemLbl = new JLabel("<html><body style='width: 150px;'>" + text + "</body></html>");
        itemLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemLbl.setForeground(new Color(70, 70, 70));
        itemLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return itemLbl;
    }

    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel title = new JLabel("Recent Activity");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(40, 40, 40));
        panel.add(title, BorderLayout.NORTH);

        activityListPanel = new JPanel();
        activityListPanel.setLayout(new BoxLayout(activityListPanel, BoxLayout.Y_AXIS));
        activityListPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(activityListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActivityRow(String icon, String text, String time) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15)); //ADDED LINE

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        JLabel textLbl = new JLabel(text);
        textLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textLbl.setForeground(new Color(60, 60, 60));

        JLabel timeLbl = new JLabel(time);
        timeLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        timeLbl.setForeground(new Color(150, 150, 150));

        row.add(iconLbl, BorderLayout.WEST);
        row.add(textLbl, BorderLayout.CENTER);
        row.add(timeLbl, BorderLayout.EAST);
        return row;
    }
}