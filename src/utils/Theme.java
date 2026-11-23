package utils;
import java.awt.Color;
import java.awt.Font;

public final class Theme {

    //Prevent instantiation
    private Theme() {}

    //Core UI Colors 
    public static final Color HEADER_BG = new Color(74, 108, 247);
    public static final Color SIDEBAR_BG = Color.WHITE;
    public static final Color SIDEBAR_BORDER = new Color(228, 230, 236);
    public static final Color CONTENT_BG = new Color(244, 246, 250);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(230, 233, 240);
    
    //Text Colors 
    public static final Color TEXT_DARK = new Color(45, 55, 72);
    public static final Color TEXT_MUTED = new Color(130, 140, 155);
    public static final Color TEXT_WHITE = Color.WHITE;

    //Button Colors 
    public static final Color BTN_GREEN = new Color(34, 139, 60);
    public static final Color BTN_RED = new Color(220, 53, 69);
    public static final Color BTN_BLUE = new Color(30, 100, 200);
    public static final Color BTN_GRAY = new Color(108, 117, 125);
    public static final Color BTN_ORANGE = new Color(230, 120, 30);
    public static final Color BTN_LIGHT_BLUE = new Color(210, 230, 245);

    //Stat Card Backgrounds 
    public static final Color STAT_BLUE_BG = new Color(228, 238, 255);
    public static final Color STAT_GREEN_BG = new Color(228, 245, 232);
    public static final Color STAT_PURPLE_BG = new Color(240, 230, 250);
    public static final Color STAT_YELLOW_BG = new Color(255, 248, 220);

    //Fonts 
    public static final Font FONT_BOLD_LARGE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
}
