package views.dashboard.client;

import dao.AppointmentDAO;
import dao.DentistDAO;
import dao.ServiceDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Appointment;
import models.Dentist;
import models.Service;
import utils.Session;
import utils.Theme; 
import views.LoginFrame;
import views.components.SidebarPanel;

//The main dashboard window for Clients.
public class ClientDashboardFrame extends JFrame {

    //Layout manager to switch between different screens
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    
    //Reusable custom Sidebar component
    private SidebarPanel sidebarPanel; 

    //Panel References
    private BookAppointmentPanel bookPanel; 
    private DentalServicesPanel servicesPanel; 
    private MyAppointmentsPanel myApptsPanel;  

    //Data Access Objects
    private final AppointmentDAO apptDAO = new AppointmentDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final DentistDAO dentistDAO = new DentistDAO(); 

    //Dynamic Labels for the Home Stats
    private final JLabel upcomingVal = new JLabel("0");
    private final JLabel servicesVal = new JLabel("0");

    //Panels for the lists
    private JPanel dentistListPanel;
    private JPanel svcListPanel;
    private JPanel apptListPanel;
    private JPanel activityListPanel; //For Recent Activity

    //Constructor
    public ClientDashboardFrame() {
        setTitle("DentDesk - Dental Clinic Appointment System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 800); //Kept the increased height for the activity panel
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.HEADER_BG); 
        header.setPreferredSize(new Dimension(0, 55));
        header.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JLabel title = new JLabel("DentDesk  -  Dental Clinic Appointment System (Client)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Theme.TEXT_WHITE); 
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 13));
        right.setOpaque(false);
        
        String displayName = "Client";
        if (Session.getCurrentUser() != null && Session.getCurrentUser().getFullName() != null) {
            displayName = Session.getCurrentUser().getFullName() + " (Client)";
        }

        JLabel who = new JLabel(displayName);
        who.setForeground(Theme.TEXT_WHITE); 
        who.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel avatar = new JLabel("\uD83D\uDC64"); 
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        right.add(who);
        right.add(avatar);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JPanel buildSidebar() {
        sidebarPanel = new SidebarPanel(this::handleNavigation);
        sidebarPanel.addItem("\uD83C\uDFE0", "Dashboard", "home", true);
        sidebarPanel.addItem("\uD83D\uDCC5", "Book Appointment", "book", false);
        sidebarPanel.addItem("\uD83E\uDDB7", "Dental Services", "services", false);
        sidebarPanel.addItem("\uD83D\uDCC6", "My Appointments", "myappt", false);
        sidebarPanel.addItem("\uD83D\uDEAA", "Logout", "logout", false);
        sidebarPanel.finish(); 
        return sidebarPanel;
    }

    private void handleNavigation(String key) {
        if ("logout".equals(key)) {
            int c = JOptionPane.showConfirmDialog(this, "Log out of DentDesk?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                Session.clear();
                dispose();
                new LoginFrame().setVisible(true);
            }
            return;
        }
        switch (key) {
            case "book":
                if (bookPanel != null) bookPanel.refresh();
                break;
            case "services":
                if (servicesPanel != null) servicesPanel.refresh();
                break;
            case "myappt":
                if (myApptsPanel != null) myApptsPanel.refresh();
                break;
            case "home":
                refreshHomeStats();
                break;
            default:
                break;
        }
        cardLayout.show(contentPanel, key);
    }

    private JPanel buildContent() {
        contentPanel.setBackground(Theme.CONTENT_BG); 
        contentPanel.add(buildHomePanel(), "home");

        bookPanel = new BookAppointmentPanel(() -> {
            cardLayout.show(contentPanel, "home");
            refreshHomeStats(); 
        });
        contentPanel.add(bookPanel, "book");

        servicesPanel = new DentalServicesPanel();
        contentPanel.add(servicesPanel, "services");

        myApptsPanel = new MyAppointmentsPanel();
        contentPanel.add(myApptsPanel, "myappt");

        refreshHomeStats();
        return contentPanel;
    }

    private JPanel buildHomePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Theme.CONTENT_BG); 
        p.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //TOP SECTION: Stats & Welcome 
        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BoxLayout(topWrapper, BoxLayout.Y_AXIS));
        topWrapper.setOpaque(false);

        //Stat Cards
        upcomingVal.setFont(new Font("Segoe UI", Font.BOLD, 24));
        upcomingVal.setForeground(Theme.TEXT_DARK);
        servicesVal.setFont(new Font("Segoe UI", Font.BOLD, 24));
        servicesVal.setForeground(Theme.TEXT_DARK);

        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        stats.add(statCard("\uD83D\uDCC5", "Upcoming Appointments", upcomingVal, Theme.STAT_BLUE_BG)); 
        stats.add(statCard("\uD83E\uDDB7", "Total Services", servicesVal, Theme.STAT_GREEN_BG)); 
        stats.add(statCard("\u23F0", "Account Status", new JLabel("Active"), Theme.STAT_PURPLE_BG)); 

        //Welcome Message
        JPanel welcome = new JPanel(new BorderLayout());
        welcome.setBackground(Theme.CARD_BG); 
        welcome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER), 
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcome.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        String name = Session.getCurrentUser() != null ? Session.getCurrentUser().getFullName() : "Client";
        JLabel h = new JLabel("Welcome back, " + name + "!");
        h.setFont(new Font("Segoe UI", Font.BOLD, 18));
        h.setForeground(Theme.TEXT_DARK); 
        text.add(h);
        text.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel hint = new JLabel("Use the Book Appointment menu on the left to schedule a new appointment, manage your bookings, or check available dental services.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hint.setForeground(Theme.TEXT_MUTED); 
        text.add(hint);

        welcome.add(text, BorderLayout.CENTER);

        topWrapper.add(stats);
        topWrapper.add(Box.createRigidArea(new Dimension(0, 16)));
        topWrapper.add(welcome);
        
        p.add(topWrapper, BorderLayout.NORTH);

        //CENTER SECTION: 3 Boxes 
        JPanel centerListsPanel = new JPanel(new GridLayout(1, 3, 16, 0));
        centerListsPanel.setOpaque(false);

        dentistListPanel = createListContentPanel();
        centerListsPanel.add(createListBox("Available Dentists", dentistListPanel));

        svcListPanel = createListContentPanel();
        centerListsPanel.add(createListBox("Clinic Services", svcListPanel));

        apptListPanel = createListContentPanel();
        centerListsPanel.add(createListBox("My Appointments", apptListPanel));

        p.add(centerListsPanel, BorderLayout.CENTER);

        //BOTTOM SECTION: Recent Activity 
        JPanel activityPanel = createActivityPanel();
        activityPanel.setPreferredSize(new Dimension(0, 130)); //Fixed height
        p.add(activityPanel, BorderLayout.SOUTH);

        return p;
    }

    private JPanel statCard(String icon, String title, JLabel valueLabel, Color bg) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(bg); 
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER), 
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JLabel ic = new JLabel(icon);
        ic.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        card.add(ic, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setForeground(Theme.TEXT_MUTED); 
        
        text.add(t);
        text.add(valueLabel); 
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private void refreshHomeStats() {
        if (Session.getCurrentUser() == null) return;
        try {
            int clientId = Session.getCurrentUser().getId();
            
            upcomingVal.setText(String.valueOf(apptDAO.countUpcomingByClient(clientId)));
            servicesVal.setText(String.valueOf(serviceDAO.countAll()));

            //Dentists
            dentistListPanel.removeAll();
            List<Dentist> dentists = dentistDAO.getAll();
            for (Dentist d : dentists) {
                if("Active".equalsIgnoreCase(d.getStatus())) {
                    dentistListPanel.add(createListItem(d.getName() + " (" + d.getSpecialization() + ")"));
                }
            }

            //Services
            svcListPanel.removeAll();
            List<Service> services = serviceDAO.getAll();
            for (Service s : services) {
                svcListPanel.add(createListItem(s.getServicesName() + " - \u20B1" + String.format("%,.2f", s.getPrice())));
            }

            //My Appointments
            apptListPanel.removeAll();
            List<Appointment> myAppts = apptDAO.getAppointmentsByClient(clientId);
            if(myAppts.isEmpty()){
                JLabel emptyLbl = new JLabel("You have no appointments yet.");
                emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                emptyLbl.setForeground(Color.GRAY);
                apptListPanel.add(emptyLbl);
            } else {
                for (Appointment a : myAppts) {
                    String text = a.getFormattedDateTime() + "<br><b>" + a.getDentistName() + "</b> - " + a.getStatus();
                    apptListPanel.add(createListItem("<html>" + text + "</html>"));
                }
            }

            //Update Recent Activity for THIS Client
            activityListPanel.removeAll();
            List<Appointment> recentAppts = apptDAO.getRecentAppointmentsByClient(clientId, 5);
            
            if (recentAppts.isEmpty()) {
                JLabel emptyLbl = new JLabel("No recent activity found.");
                emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                emptyLbl.setForeground(Theme.TEXT_MUTED);
                activityListPanel.add(emptyLbl);
            } else {
                for (Appointment a : recentAppts) {
                    String icon = "\uD83D\uDCC5"; //Default calendar
                    String text = "You booked " + a.getServiceName() + " with " + a.getDentistName();
                    
                    //Change text/icon based on the status
                    if ("Completed".equalsIgnoreCase(a.getStatus())) {
                        icon = "\u2705"; //Checkmark
                        text = "Your visit with " + a.getDentistName() + " is completed.";
                    } else if ("Cancelled".equalsIgnoreCase(a.getStatus())) {
                        icon = "\u274C"; //X mark
                        text = "Your appt with " + a.getDentistName() + " was cancelled.";
                    } else if ("Approved".equalsIgnoreCase(a.getStatus())) {
                        icon = "\uD83D\uDC4D"; //Thumbs up
                        text = "Your appt with " + a.getDentistName() + " was approved.";
                    }

                    String time = a.getFormattedDateTime(); 
                    activityListPanel.add(createActivityRow(icon, text, time));
                    activityListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }

            revalidate();
            repaint();
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //UI HELPER METHODS FOR THE LISTS & ACTIVITY PANEL
    private JPanel createListBox(String title, JPanel listContent) {
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.setBackground(Theme.CARD_BG);
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Theme.TEXT_DARK);
        box.add(titleLbl, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(listContent);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CARD_BG);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        box.add(scroll, BorderLayout.CENTER);

        return box;
    }

    private JPanel createListContentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.CARD_BG);
        return panel;
    }

    private JLabel createListItem(String text) {
        JLabel itemLbl = new JLabel("<html><body style='width: 150px;'>" + text + "</body></html>");
        itemLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemLbl.setForeground(Theme.TEXT_MUTED);
        itemLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return itemLbl;
    }

    //Creates the Recent Activity box
    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Theme.CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel title = new JLabel("Recent Activity");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Theme.TEXT_DARK);
        panel.add(title, BorderLayout.NORTH);

        //Initialize the dynamic list panel
        activityListPanel = new JPanel();
        activityListPanel.setLayout(new BoxLayout(activityListPanel, BoxLayout.Y_AXIS));
        activityListPanel.setBackground(Theme.CARD_BG);

        JScrollPane scroll = new JScrollPane(activityListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CARD_BG);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    //Creates a single row inside the Recent Activity box
    private JPanel createActivityRow(String icon, String text, String time) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.CARD_BG);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15)); //ADDED LINE TO SPACE FROM SCROLLBAR

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));

        JLabel textLbl = new JLabel(text);
        textLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textLbl.setForeground(Theme.TEXT_DARK);

        JLabel timeLbl = new JLabel(time);
        timeLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        timeLbl.setForeground(Theme.TEXT_MUTED);

        row.add(iconLbl, BorderLayout.WEST);
        row.add(textLbl, BorderLayout.CENTER);
        row.add(timeLbl, BorderLayout.EAST);
        return row;
    }
}