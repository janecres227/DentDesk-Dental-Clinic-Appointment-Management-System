package utils;
import models.User;

public class Session {
    private static User currentUser; //For the current logged-in user

    public static void setCurrentUser(User user) { currentUser = user; } //Set the current logged-in user
    public static User getCurrentUser() { return currentUser; } //Get the current logged-in user
    public static void clear() { currentUser = null; } //Clear the current logged-in user

    //True only for the top-level Administrator.
    public static boolean isAdministrator() {
        return currentUser != null
                && "Administrator".equalsIgnoreCase(currentUser.getRole());
    }

    //True for regular Staff accounts.
    public static boolean isStaff() {
        return currentUser != null
                && "Staff".equalsIgnoreCase(currentUser.getRole());
    }

    //True for either admin role or staff role.
    //The Administrator is included because they can perform every
    //action a Staff can perform.
    public static boolean isAdmin() {
        return isAdministrator() || isStaff();
    }

    //True for regular Client accounts.
    public static boolean isClient() {
        return currentUser != null
                && "Client".equalsIgnoreCase(currentUser.getRole());
    }
}