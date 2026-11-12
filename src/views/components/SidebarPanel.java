package views.components;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import utils.Theme;
import views.dashboard.SidebarItem;

/*It manages the vertical layout of the SidebarItems and handles the logic 
of switching the visual "active" state when a user clicks a menu item.*/
public class SidebarPanel extends JPanel {

    //Tracks the currently selected/highlighted menu item
    private SidebarItem activeItem;
    
    //A callback function that sends the clicked menu's "key" back to the Main Frame
    private final Consumer<String> onItemClick; 

     //Constructor that sets up the sidebar's visual styling and stores the click handler.
     public SidebarPanel(Consumer<String> onItemClick) {
        this.onItemClick = onItemClick;
        
        //Setup the panel's visual style
        setBackground(Theme.SIDEBAR_BG);
        setPreferredSize(new Dimension(215, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.SIDEBAR_BORDER));
        
        //Add a little space at the top
        add(Box.createRigidArea(new Dimension(0, 12)));
    }

    //Creates a new navigation item, adds it to the sidebar, and attaches the click logic.
    //The emoji/icon and text label to display.
     public void addItem(String icon, String text, String key, boolean active) {
        SidebarItem item = new SidebarItem(icon, text, active);
        
        //If this item is set to active by default, remember it
        if (active) {
            activeItem = item;
        }

        //Attach the mouse click listener
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                //Update the colors (turn off old active item, turn on new one)
                highlightActive(item);
                
                //Trigger the callback to tell the Main Frame to switch panels
                onItemClick.accept(key);
            }
        });

        //Add the item to the sidebar layout
        add(item);
    }

    //Call this method after adding all items to push them to the top of the sidebar.
    //Without this, the BoxLayout will spread the items out evenly
    public void finish() {
        add(Box.createVerticalGlue());
    }

     //Updates the visual state of all items, making only the selected one active
    private void highlightActive(SidebarItem current) {
        //If an item was already active, turn it off
        if (activeItem != null) {
            activeItem.setActive(false);
        }
        
        //Set the new item as active
        activeItem = current;
        current.setActive(true);
        
        //Refresh the UI to show the color changes
        repaint();
    }
}
