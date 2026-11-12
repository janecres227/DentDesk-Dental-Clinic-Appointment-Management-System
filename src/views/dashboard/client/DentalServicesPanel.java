package views.dashboard.client;

import dao.ServiceDAO;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Service;
import utils.Theme;

//Panel for Clients to view the dental services offered by the clinic.
public class DentalServicesPanel extends JPanel {

    private final ServiceDAO dao = new ServiceDAO();
    
    //UI Components
    private final JPanel gridPanel;
    private final JTextArea recordsArea = new JTextArea();
    private final JTextField searchField;

    //Constructor that ets up the layout (Header, Center Grid, Bottom Records).
    public DentalServicesPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Top Header (Title on Left, Search on Right)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        //Title Label
        JLabel header = new JLabel("Available Dental Services");
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(Theme.TEXT_DARK);
        headerPanel.add(header, BorderLayout.WEST);

        //Search Bar Area
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);

        searchField = new JTextField(15);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(Theme.BTN_BLUE);
        searchBtn.setForeground(Theme.TEXT_WHITE);
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchBtn.setFocusPainted(false);
        searchBtn.setBorderPainted(false);
        searchBtn.setOpaque(true);
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));

        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        //Center Layout (Grid of Cardss)
        JPanel middle = new JPanel(new BorderLayout(0, 14));
        middle.setOpaque(false);

        //The Grid for Cards (4 columns)
        gridPanel = new JPanel(new GridLayout(0, 4, 15, 15));
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

        //Bottom Records Card
        JPanel recordsCard = new JPanel(new BorderLayout(0, 8));
        recordsCard.setBackground(Theme.CARD_BG);
        recordsCard.setPreferredSize(new Dimension(0, 90));
        recordsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel recHeader = new JLabel("Service Records");
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

        //Action Listeners
        searchBtn.addActionListener(e -> onSearch());
        
        //Allow pressing "Enter" in the text field to trigger search
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onSearch();
                }
            }
        });

        //Initial Load
        refresh();
    }

    //Data Operations
    //Public method called by the ClientDashboardFrame to reload all services.
    //Also clears the search bar text.
    public void refresh() {
        searchField.setText(""); //Clear search field
        try {
            //Get all services from the database and display them
            populateGrid(dao.getAll());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load services: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Handles the search logic when the user clicks the Search button or presses Enter.
    private void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refresh(); //Load all if the search field is empty
            return;
        }

        try {
            //Get the filtered services from the database
            List<Service> results = dao.search(keyword);
            populateGrid(results);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Clears the current grid and fills it with visual cards based on the provided list.
    private void populateGrid(List<Service> list) {
        gridPanel.removeAll(); //Clear old cards
        StringBuilder sb = new StringBuilder();

        for (Service s : list) {
            ServiceCard card = new ServiceCard(s);
            gridPanel.add(card);

            //Build the text summary for the bottom panel
            sb.append("[").append(String.format("S%03d", s.getId())).append(" - ")
              .append(s.getServicesName()).append(" - \u20B1")
              .append(String.format("%.2f", s.getPrice())).append("]  ");
        }
        
        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    //Inner Class: Service Card
    //Custom UI component representing a single service as a card.
    //Displays the Service ID, Name, and Price vertically.
    private class ServiceCard extends JPanel {
        public ServiceCard(Service s) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            
            //Uniform height for the cards
            setPreferredSize(new Dimension(200, 100));

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

            //Assemble vertically and center everything
            add(Box.createVerticalGlue());
            add(idLbl);
            add(Box.createRigidArea(new Dimension(0, 6)));
            add(nameLbl);
            add(Box.createRigidArea(new Dimension(0, 6)));
            add(priceLbl);
            add(Box.createVerticalGlue());
        }
    }
}