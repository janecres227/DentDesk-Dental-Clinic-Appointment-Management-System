package views.dashboard;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import utils.Theme; 

//A reusable UI component representing a single clickable item in the sidebar navigation.
//Handles its own hover and active visual states.
public class SidebarItem extends JPanel {
    
    //Flag tracking whether this nav item is currently active
    private boolean active; 
    
    //UI elements for the navigation item
    private final JLabel iconLabel;
    private final JLabel textLabel;
    
    //Constructor that initializes the sidebar item with an icon, text, and initial active state.
    public SidebarItem(String icon, String text, boolean active) {
        this.active = active; 
        
        //Setup basic layout and sizing
        setLayout(new BorderLayout(10, 0)); //10px gap between icon and text
        setBorder(BorderFactory.createEmptyBorder(11, 20, 11, 14)); //Padding
        setOpaque(true); //Required to show background colors
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 48)); //Max height
        
        //Setup the Icon Label
        iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        iconLabel.setPreferredSize(new Dimension(24, 20));
        
        //Setup the Text Label
        textLabel = new JLabel(text);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        //Assemble the component
        add(iconLabel, BorderLayout.WEST);
        add(textLabel, BorderLayout.CENTER);
        
        //Apply initial colors
        applyState();
        
        //Add mouse listener for interactive hover effects
        addMouseListener(new MouseAdapter() {
            @Override 
            public void mouseEntered(MouseEvent e) {
                //Highlight background on hover, but ONLY if the item is not currently active
                if (!SidebarItem.this.active) {
                    setBackground(Theme.STAT_BLUE_BG); //Using a light blue from Theme
                }
            }
            
            @Override 
            public void mouseExited(MouseEvent e) {
                //Revert background when mouse leaves, but ONLY if it's not the active item
                if (!SidebarItem.this.active) {
                    setBackground(Theme.SIDEBAR_BG); //Revert to white
                }
            }
        });
    }

    //Updates the visual styling (colors) based on whether this item is currently selected (active).
    private void applyState() {
        if (active) {
            //ACTIVE STATE (Selected) 
            setBackground(Theme.HEADER_BG); //Blue background matching the header
            iconLabel.setForeground(Theme.TEXT_WHITE);
            textLabel.setForeground(Theme.TEXT_WHITE);
        } else {
            //INACTIVE STATE (Normal) 
            setBackground(Theme.SIDEBAR_BG); //White background
            iconLabel.setForeground(Theme.TEXT_MUTED); //Gray icon
            textLabel.setForeground(Theme.TEXT_DARK);  //Dark text
        }
    }
    
    //Sets the active state of this sidebar item and triggers an immediate visual update.
    public void setActive(boolean active) {
        this.active = active;
        applyState(); //Refresh the colors immediately
    }
}