package dao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import models.ClientRow;
import models.User;

//Data Access Object (DAO) for managing User records in the database
public class UserDAO {

    //Check if a username and password match a user in the database. Returns the User object if successful, or null if not.
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password, role, full_name, email, phone " +
                     "FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");

                    if (stored.equals(password)) {
                        return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            stored,
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone")
                        );
                    }
                }
            }
        }
        return null;
    }

    //Insert new User along with their details into the database. 
    public boolean register(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, full_name, email, phone) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());   
            ps.setString(3, user.getRole());
            ps.setString(4, user.getFullName());
            
            //Normalize email to lowercase before saving
            String safeEmail = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase();
            ps.setString(5, safeEmail);
            
            ps.setString(6, user.getPhone());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    //Validates if a username already exists in the database (Case-Insensitive).
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    //Validates if an email already exists in the database (Case-Insensitive).
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    //Count users with a given role (e.g. "Client", "Administrator", "Staff"). 
    public int countByRole(String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    //Count all clients including registered users and walk-in clients, ensuring no duplicates based on name. 
    public int countAllClients() throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM ( " +
            "    SELECT full_name AS name FROM users WHERE role = 'Client' " +
            "    UNION " +
            "    SELECT DISTINCT walk_in_name AS name FROM appointments " +
            "      WHERE walk_in_name IS NOT NULL AND walk_in_name <> '' " +
            ") AS combined";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    //Get all users with a given role, sorted by full name.
    public List<User> getAllByRole(String role) throws SQLException {
        String sql = "SELECT id, username, password, role, full_name, email, phone " +
                     "FROM users WHERE role = ? ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone")
                    ));
                }
            }
        }
        return list;
    }

    //Client listing for the Manage Clients panel
    //Registered clients come first (by user id), then walk-ins (alphabetical).
    private static final String BASE_CLIENT_SELECT =
        "SELECT user_id, name, phone, email, status FROM ( " +
        "    SELECT u.id AS user_id, u.full_name AS name, u.phone, u.email, " +
        "           'Active' AS status " +
        "    FROM users u WHERE u.role = 'Client' " +
        "    UNION ALL " +
        "    SELECT NULL AS user_id, w.walk_in_name AS name, " +
        "           w.walk_in_phone AS phone, " +
        "           NULL AS email, 'Walk-in' AS status " +
        "    FROM ( " +
        "        SELECT walk_in_name, " +
        "               SUBSTRING_INDEX( " +
        "                   GROUP_CONCAT(walk_in_phone ORDER BY id DESC SEPARATOR '|'), " +
        "                   '|', 1 " +
        "               ) AS walk_in_phone " +
        "        FROM appointments " +
        "        WHERE walk_in_name IS NOT NULL AND walk_in_name <> '' " +
        "        GROUP BY walk_in_name " +
        "    ) w " +
        "    WHERE NOT EXISTS ( " +
        "        SELECT 1 FROM users u2 " +
        "        WHERE u2.role = 'Client' " +
        "          AND LOWER(u2.full_name) = LOWER(w.walk_in_name) " +
        "    ) " +
        ") AS combined ";

    //All clients (registered + walk-ins), sorted registered-first then alphabetically for walk-in.
    public List<ClientRow> getAllClients() throws SQLException {
        String sql = BASE_CLIENT_SELECT + "ORDER BY user_id IS NULL, user_id, name";
        return queryClients(sql);
    }

    //Search registered clients by name, phone, or email.
    //Search walk-ins by name or phone number only.
    public List<ClientRow> searchClients(String keyword) throws SQLException {
        String sql = "SELECT * FROM (" + BASE_CLIENT_SELECT + ") AS all_clients " +
                     "WHERE name LIKE ? OR phone LIKE ? OR email LIKE ? " +
                     "ORDER BY user_id IS NULL, user_id, name";
        List<ClientRow> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapClientRow(rs));
            }
        }
        return list;
    }

    //Runs the given SQL and returns all matching rows as ClientRow objects.
    private List<ClientRow> queryClients(String sql) throws SQLException {
        List<ClientRow> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapClientRow(rs));
        }
        return list;
    }

    //Converts one ResultSet row into a ClientRow. A NULL user_id means it's a walk-in.
    private ClientRow mapClientRow(ResultSet rs) throws SQLException {
        int rawId = rs.getInt("user_id");
        boolean isNull = rs.wasNull();
        Integer userId = isNull ? null : rawId;
        ClientRow.Type type = isNull ? ClientRow.Type.WALK_IN : ClientRow.Type.REGISTERED;
        return new ClientRow(
                type,
                userId,
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"));
    }

    //Remove/Delete a user by id. 
    public boolean deleteById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    //Manage Staff for the Admin Panel: Update an existing staff member overwrites every column.
    public boolean updateById(User user) throws SQLException {
        String sql = "UPDATE users SET username=?, password=?, role=?, " +
                     "full_name=?, email=?, phone=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());   
            ps.setString(3, user.getRole());
            ps.setString(4, user.getFullName());
            
            //Normalize email to lowercase before saving
            String safeEmail = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase();
            ps.setString(5, safeEmail);
            
            ps.setString(6, user.getPhone());
            ps.setInt(7, user.getId());
            return ps.executeUpdate() > 0;
        }
    }

    //Checks if another user already has this username (excluding one id).
    //Used during edit so the user's own username doesn't trigger "already taken." (Case-Insensitive)
    public boolean usernameExistsExcept(String username, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) AND id <> ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    //Checks if another user already has this email (ignores one user id).
    //Used during edit so the user's own email doesn't trigger "already registered." (Case-Insensitive)
    public boolean emailExistsExcept(String email, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) AND id <> ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    //Used by the Settings panel to allow the Admin to change their password securely.
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword); 
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}