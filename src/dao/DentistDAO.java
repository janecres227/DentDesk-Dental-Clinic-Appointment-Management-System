package dao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import models.Dentist;

public class DentistDAO {
    
    //Return a list of all dentists in the database, ordered by ID.
    public List<Dentist> getAll() throws SQLException {
        String sql = "SELECT id, name, specialization, status FROM dentists ORDER BY id";
        List<Dentist> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Dentist(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("specialization"),
                        rs.getString("status")));
            }
        }
        return list;
    }

    //Search for dentists by id, name, or specialization.
    public List<Dentist> search(String keyword) throws SQLException {
        String sql = "SELECT id, name, specialization, status FROM dentists " +
                     "WHERE name LIKE ? OR specialization LIKE ? ORDER BY id";
        List<Dentist> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Dentist(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("specialization"),
                            rs.getString("status")));
                }
            }
        }
        return list;
    }
    
    //Add a new dentist to the database. If successful, set the generated ID in the Dentist object.
    public boolean insert(Dentist d) throws SQLException {
        String sql = "INSERT INTO dentists (name, specialization, status) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getSpecialization());
            ps.setString(3, d.getStatus());
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) d.setId(keys.getInt(1));
                }
                return true;
            }
        }
        return false;
    }
    
    //Update an existing dentist's information in the database.
    public boolean update(Dentist d) throws SQLException {
        String sql = "UPDATE dentists SET name=?, specialization=?, status=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getSpecialization());
            ps.setString(3, d.getStatus());
            ps.setInt(4, d.getId());
            return ps.executeUpdate() > 0;
        }
    } 

    //Delete a dentist by ID.
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM dentists WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    } 

    //Count the total number of dentists in the database.
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dentists";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
