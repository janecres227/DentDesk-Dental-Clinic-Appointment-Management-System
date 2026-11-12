package views.dashboard.admin;

import dao.AppointmentDAO;
import dao.DentistDAO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;
import models.Dentist;
import utils.Theme;

//Panel for Admin/Staff to manage Dentists
//Clicking a card selects it and loads its data into the form for editing
public class ManageDentistsPanel extends JPanel {

    private final DentistDAO dao = new DentistDAO();
    private final AppointmentDAO apptDAO = new AppointmentDAO(); 
    private final JPanel gridPanel = new JPanel(new GridLayout(0, 3, 15, 15));
    private final JTextArea recordsArea = new JTextArea();
    
    //Tracks which visual card is currently selected by the user
    private DentistCard selectedCard = null;

    //Right-Side Form Fields
    private final JTextField nameField;
    private final JTextField specField;
    private final JComboBox<String> statusBox;
    private JButton saveBtn;
    private JButton clearBtn;

    //Drop-down search for Dentists
    private JComboBox<String> dentistSearchBox;
    private boolean isUpdatingDropdown = false;
    
    //Tracks if we are inserting (null) or updating (has ID) a dentist
    private Integer editingId = null;

    //Constructor that initializes the form fields, builds the UI layout, and loads data.
    public ManageDentistsPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.CONTENT_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        //Initialize Form Fields
        nameField = textField("Enter dentist name");
        specField = textField("Enter specialization");
        
        statusBox = new JComboBox<>(new String[]{"Active", "Inactive"});
        statusBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusBox.setBackground(Theme.CARD_BG);
        statusBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        statusBox.setPreferredSize(new Dimension(0, 34));

        //Main Layout Assembly (Grid on Left, Form on Right, Records on Bottom)
        JPanel middle = new JPanel(new BorderLayout(16, 0));
        middle.setOpaque(false);
        middle.add(buildManageCard(), BorderLayout.CENTER);
        middle.add(buildInfoCard(),   BorderLayout.EAST);
        
        add(middle, BorderLayout.CENTER);
        add(buildRecordsCard(), BorderLayout.SOUTH);

        //Initial Data Load & Lock Form
        refresh();
        setFormEnabled(false);
    }

    //Left Side: Card Grid & Toolbar
    //Builds the left-side panel containing the toolbar buttons and the card grid
    private JPanel buildManageCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Manage Dentists");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        //Toolbar Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton insertBtn = actionBtn("+ Insert Dentist", Theme.BTN_GREEN);
        JButton removeBtn = actionBtn("\uD83D\uDDD1 Remove Dentist", Theme.BTN_RED);
        JButton searchBtn = actionBtn("\uD83D\uDD0D Search Dentist", Theme.BTN_BLUE);
        JButton allBtn = actionBtn("\u2630 Display All", Theme.BTN_GRAY);
        
        btnRow.add(insertBtn);
        btnRow.add(removeBtn);
        btnRow.add(searchBtn);
        btnRow.add(allBtn);

        //DENTIST DROP-DOWN SEARCH 
        dentistSearchBox = new JComboBox<>();
        dentistSearchBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dentistSearchBox.setBackground(Theme.CARD_BG);
        dentistSearchBox.setFocusable(false);
        dentistSearchBox.setPreferredSize(new Dimension(150, 26)); //Make it look neat
        btnRow.add(dentistSearchBox);
        
        //Listen for dropdown changes
        dentistSearchBox.addActionListener(e -> {
            if (!isUpdatingDropdown) {
                onDentistSelected();
            }
        });
        
        body.add(btnRow, BorderLayout.NORTH);
        //Scrollable Grid for DentistCards
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

        //Action Listeners
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
    //Builds the right-side panel containing the insert/update form.
    private JPanel buildInfoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(320, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel header = new JLabel("Dentist Information (Insert / Update)");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setForeground(Theme.TEXT_DARK);
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(formLabel("Name"));
        form.add(nameField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Specialization"));
        form.add(specField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(formLabel("Status"));
        form.add(statusBox);
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

    //Builds the bottom panel that displays a text summary of all dentists
    private JPanel buildRecordsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Theme.CARD_BG);
        card.setPreferredSize(new Dimension(0, 90));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel header = new JLabel("Dentist Records");
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
    //Public method called by the AdminDashboardFrame to reload all data.
    public void refresh() {
        try {
            List<Dentist> all = dao.getAll();

            //Populate the Dropdown
            isUpdatingDropdown = true;
            dentistSearchBox.removeAllItems();
            dentistSearchBox.addItem("All Dentists");
            for (Dentist d : all) {
                dentistSearchBox.addItem(d.getName());
            }
            dentistSearchBox.setSelectedIndex(0);
            isUpdatingDropdown = false;

            //Populate the Grid
            populateGrid(all);

        } catch (Exception ex) {
            showError("Could not load dentists", ex);
        }
    }

    //Renders the list of dentists into the visual grid and updates the text summary
    private void populateGrid(List<Dentist> list) {
        gridPanel.removeAll();
        selectedCard = null; //Reset selection when grid reloads
        StringBuilder sb = new StringBuilder();

        for (Dentist d : list) {
            DentistCard card = new DentistCard(d);
            gridPanel.add(card);
            
            sb.append("[").append(d.getFormattedId()).append(" - ")
              .append(d.getName()).append(", ")
              .append(d.getSpecialization()).append(", ")
              .append(d.getStatus()).append("]    ");
        }
        recordsArea.setText(sb.toString().trim());
        gridPanel.revalidate();
        gridPanel.repaint();
    }
    
    //Validates the form and either INSERTs a new dentist or UPDATEs an existing one
    private void onSaveFromForm() {
        String name = nameField.getText().trim();
        String spec = specField.getText().trim();
        String status = (String) statusBox.getSelectedItem(); //Capture status early
        
        //Validation
        if (name.isEmpty() || "Enter dentist name".equals(name)) {
            JOptionPane.showMessageDialog(this, "Please enter a name.");
            return;
        }
        if (spec.isEmpty() || "Enter specialization".equals(spec)) {
            JOptionPane.showMessageDialog(this, "Please enter a specialization.");
            return;
        }

        try {
            if (editingId == null) {
                //Insert Mode
                dao.insert(new Dentist(0, name, spec, status));
                JOptionPane.showMessageDialog(this, "Dentist added successfully.");
            } else {
                //Update Mode
                dao.update(new Dentist(editingId, name, spec, status));
                
                //If status is set to "Inactive", auto-cancel today's approved appointments
                if ("Inactive".equalsIgnoreCase(status)) {
                    int cancelledCount = apptDAO.cancelTodayApprovedAppointmentsForDentist(editingId);
                    if (cancelledCount > 0) {
                        JOptionPane.showMessageDialog(this, 
                                "Dentist status changed to Inactive.\n\n" +
                                cancelledCount + " approved appointment(s) for today were automatically cancelled.", 
                                "Appointments Auto-Cancelled", JOptionPane.WARNING_MESSAGE);
                    }
                }
                JOptionPane.showMessageDialog(this, "Dentist updated successfully.");
            }
            
            editingId = null;
            clearForm();
            refresh();
            setFormEnabled(false);
        } catch (Exception ex) {
            showError("Save failed", ex);
        }
    }

    //Deletes the currently selected dentist card.
    //Includes a specific catch block to prevent deletion if the dentist has schedules/appointments.
    private void onRemovePrompt() {
        if (selectedCard == null) {
            JOptionPane.showMessageDialog(this, "Please click a dentist box first to select it.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Dentist d = selectedCard.getDentist();
        int c = JOptionPane.showConfirmDialog(this, "Delete dentist " + d.getFormattedId() + " (" + d.getName() + ")?", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        try {
            dao.delete(d.getId());
            refresh();
        } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
            //Handles the Foreign Key constraint error
            JOptionPane.showMessageDialog(this, "Cannot delete this dentist because they have existing schedules or appointments.", "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            showError("Delete failed", ex);
        }
    }

    //Prompts the user for a keyword and filters the dentist grid
    private void onSearch() {
        String q = JOptionPane.showInputDialog(this, "Search by name or specialization:");
        if (q == null) return;
        if (q.trim().isEmpty()) { refresh(); return; }
        try {
            populateGrid(dao.search(q.trim()));
        } catch (Exception ex) {
            showError("Search failed", ex);
        }
    }

    //Filters the grid based on the selected dentist in the dropdown
    private void onDentistSelected() {
        String selectedName = (String) dentistSearchBox.getSelectedItem();
        
        //If "All Dentists" is selected, reload everything
        if (selectedName == null || "All Dentists".equals(selectedName)) {
            try {
                populateGrid(dao.getAll());
            } catch (Exception ex) {
                showError("Load failed", ex);
            }
            return;
        }
        
        try {
            List<Dentist> all = dao.getAll();
            List<Dentist> filtered = new ArrayList<>();
            
            for (Dentist d : all) {
                if (d.getName() != null && d.getName().equalsIgnoreCase(selectedName)) {
                    filtered.add(d);
                }
            }
            populateGrid(filtered);
        } catch (Exception ex) {
            showError("Filter failed", ex);
        }
    }

    //Helpers & Styling
    //Clears all text fields and resets them to their placeholder state
    private void clearForm() {
        nameField.setText("Enter dentist name");
        nameField.setForeground(Theme.TEXT_MUTED);
        specField.setText("Enter specialization");
        specField.setForeground(Theme.TEXT_MUTED);
        statusBox.setSelectedIndex(0);
    }

    //Locks or unlocks the right-side form components.
    private void setFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        specField.setEnabled(enabled);
        statusBox.setEnabled(enabled);
        if (saveBtn != null) saveBtn.setEnabled(enabled);
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

    //Creates a standard action button(Insert, Remove, Search, Display All)
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

    //Dentist Card
    //Custom UI component representing a single dentist as a clickable card
    //Clicking this card highlights it and populates the right-side form for editing
    private class DentistCard extends JPanel {
        private final Dentist dentist;

        public DentistCard(Dentist dentist) {
            this.dentist = dentist;
            
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CARD_BORDER, 1),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 140));

            //Labels
            JLabel name = new JLabel(dentist.getName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 14));
            name.setForeground(Theme.TEXT_DARK);
            name.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel id = new JLabel(dentist.getFormattedId());
            id.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            id.setForeground(Theme.TEXT_MUTED);
            id.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel spec = new JLabel(dentist.getSpecialization());
            spec.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            spec.setForeground(Theme.TEXT_DARK);
            spec.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel status = new JLabel(dentist.getStatus());
            status.setFont(new Font("Segoe UI", Font.BOLD, 11));
            status.setForeground("Active".equals(dentist.getStatus()) ? Theme.BTN_GREEN : Theme.BTN_RED);
            status.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Assemble vertically
            add(name);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(id);
            add(Box.createRigidArea(new Dimension(0, 8)));
            add(spec);
            add(Box.createRigidArea(new Dimension(0, 8)));
            add(status);

            //Click listener for selection and editing
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    //Deselect previous card
                    if (selectedCard != null && selectedCard != DentistCard.this) {
                        selectedCard.setSelected(false);
                    }
                    setSelected(true);
                    selectedCard = DentistCard.this;
                    
                    //Populate the right-side form with this dentist's data
                    editingId = dentist.getId();
                    nameField.setText(dentist.getName());
                    nameField.setForeground(Theme.TEXT_DARK);
                    specField.setText(dentist.getSpecialization());
                    specField.setForeground(Theme.TEXT_DARK);
                    statusBox.setSelectedItem(dentist.getStatus());
                    setFormEnabled(true);
                }
            });
        }

        //Toggles the visual highlight (thick blue border) when selected.
        public void setSelected(boolean selected) {
            if (selected) {
                setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(Theme.BTN_BLUE, 2),
                        BorderFactory.createEmptyBorder(14, 14, 14, 14)));
            } else {
                setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(Theme.CARD_BORDER, 1),
                        BorderFactory.createEmptyBorder(15, 15, 15, 15)));
            }
        }

        public Dentist getDentist() {
            return dentist;
        }
    }
}