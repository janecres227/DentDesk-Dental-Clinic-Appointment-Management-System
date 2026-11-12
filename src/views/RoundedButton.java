package views;
import java.awt.*;
import javax.swing.*;


//A custom JButton implementation that draws a rounded rectangle background.
//This replaces the standard, sharp-edged Swing button to give the UI a modern look.
public class RoundedButton extends JButton {
    
    //The radius determines how round the corners of the button will be
    private final int radius;

    //Constructor to initialize the button's text and corner radius.
    public RoundedButton(String text, int radius) {
        super(text);
        this.radius = radius;
        
        //Crucial setup for custom painting
        //Disable the default filled background area
        setContentAreaFilled(false); 
        //Disable the dotted focus rectangle around the text
        setFocusPainted(false);     
        //Disable the default standard Swing border (which is square)
        setBorderPainted(false);    
        //Make the button non-opaque so our custom background shows properly
        setOpaque(false);           
    }

    //The core method that draws the button on the screen.
     @Override
    protected void paintComponent(Graphics g) {
        //Cast to Graphics2D for advanced drawing (smooth edges, custom shapes)
        Graphics2D g2 = (Graphics2D) g.create();
        
        //Enable Anti-Aliasing to smooth out the curves of the rounded corners
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //Draw the button's background
        //We get the background color assigned to the button and fill a rounded rectangle
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        
        //Draw a border ONLY if the button is white (e.g., secondary buttons like "Clear" or "Admin Login")
        //This gives white buttons a visible outline so they don't blend into the background.
        if (getBackground().equals(Color.WHITE)) {
            g2.setColor(new Color(13, 110, 253)); // A standard blue outline
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        
        //Call the parent's paintComponent to draw the text and icons on top of the background
        super.paintComponent(g2);
        
        //Clean up the Graphics context to free memory
        g2.dispose();
    }
}