package dao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import models.Service;

public class ServiceDAO {

    //Get all services, ordered by name.
    public List<Service> getAll() throws SQLException {
        return query("SELECT id, services_name, description, price FROM services ORDER BY services_name");
    }

    //Search for services by name, returning a list of matching services.
    public List<Service> search(String keyword) throws SQLException {
        //Updated to be case-insensitive
        String sql = "SELECT id, services_name, description, price FROM services " +
                     "WHERE LOWER(services_name) LIKE LOWER(?) ORDER BY services_name";
        List<Service> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    //Returns service list from ResultSet, mapping each row to a Services object.
    //Use for sql queries for CRUD operations.
    private List<Service> query(String sql) throws SQLException {
        List<Service> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    //Map a ResultSet row to a Services object, extracting the ID, name, description, and price.
    private Service mapRow(ResultSet rs) throws SQLException {
        return new Service(
                rs.getInt("id"),
                rs.getString("services_name"),
                rs.getString("description"),
                rs.getDouble("price"));
    }

    //Add a new service to the database. If successful, set the generated ID in the Services object.
    public boolean insert(Service s) throws SQLException {
        String sql = "INSERT INTO services (services_name, description, price) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getServicesName());
            ps.setString(2, s.getDescription());
            ps.setDouble(3, s.getPrice());
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) s.setId(keys.getInt(1));
                }
                return true;
            }
        }
        return false;
    }

    //Update an existing dental service in the database.
    public boolean update(Service s) throws SQLException {
        String sql = "UPDATE services SET services_name=?, description=?, price=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getServicesName());
            ps.setString(2, s.getDescription());
            ps.setDouble(3, s.getPrice());
            ps.setInt(4, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    //Delete a dental service by ID.
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM services WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    //Count the total number of dental services in the database.
    public int countAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM services");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    //METHODS FOR DUPLICATE VALIDATION
    //Checks if a service name already exists in the database (Case-Insensitive).
    public boolean serviceNameExists(String serviceName) throws SQLException {
        String sql = "SELECT 1 FROM services WHERE LOWER(services_name) = LOWER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serviceName);
            try (ResultSet rs = ps.executeQuery()) { 
                return rs.next(); 
            }
        }
    }

    //Checks if ANOTHER service already has this name (ignores the current service ID being edited).
    public boolean serviceNameExistsExcept(String serviceName, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM services WHERE LOWER(services_name) = LOWER(?) AND id <> ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serviceName);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { 
                return rs.next(); 
            }
        }
    }
}