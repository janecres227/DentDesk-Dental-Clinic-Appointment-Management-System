package views;
import java.awt.*;
import javax.swing.border.Border;

//A custom Border implementation that draws a rounded rectangle around a component.
//This is used to give standard Swing components (like JTextField) a modern, pill-like appearance.
public class RoundedBorder implements Border {
    
    //The radius determines how round the corners will be (higher = more rounded)
    private int radius;

    //Constructor to initialize the border with a specific corner radius.
    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    //Defines the padding (space) that the border takes up around the component.
    //This ensures the text inside the field doesn't overlap with the rounded corners.
    @Override
    public Insets getBorderInsets(Component c) {
        //Returns padding for Top, Left, Bottom, Right
        return new Insets(this.radius + 1, this.radius + 1, this.radius + 2, this.radius);
    }

    //Tells Swing whether the border itself is completely opaque (fills its background).
    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    //The core method that actually draws the border on the screen.
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        //Cast the standard Graphics object to Graphics2D for advanced rendering features
        Graphics2D g2 = (Graphics2D) g;
        
        //Enable Anti-Aliasing. This smooths out the edges of the rounded corners so they don't look pixelated.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //et the drawing color. It uses the component's Foreground color (text color).
        g2.setColor(c.getForeground()); 
        
        //Draw the rounded rectangle.
        //width - 1 and height - 1 ensure the line stays within the component's boundaries.
        //radius, radius define the width and height of the arc for the corners.
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
    }
}