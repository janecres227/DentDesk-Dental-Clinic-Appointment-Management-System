package dao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/dentdesk_db";
    private static final String USER = "dentdesk_app";
    private static final String PASSWORD = "aiwprton";
    
    //Static initializer block to load the MySQL JDBC driver.
    //Run once when the class is loaded to ensure the driver is available.
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC Driver not found. " + e.getMessage());
        }
    }

    //Utility class — no instances. 
    private DBConnection() { }

    
     //Opens a new connection to the DentDesk database.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
