package dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import models.Appointment;

public class AppointmentDAO {
    
    //Base SELECT query for appointments with joins to users, dentists, and services
    private static final String BASE_SELECT =
        "SELECT a.id, a.client_id, " +
        "       COALESCE(u.full_name, a.walk_in_name) AS client_name, " +
        "       a.walk_in_name, a.walk_in_phone, " +
        "       a.dentist_id, d.name AS dentist_name, " +
        "       a.service_id, s.services_name AS service_name, " +
        "       a.appointment_date, a.appointment_time, " +
        "       a.status, a.notes " +
        "FROM appointments a " +
        "LEFT JOIN users u ON u.id = a.client_id " +
        "JOIN dentists d ON d.id = a.dentist_id " +
        "LEFT JOIN services s ON s.id = a.service_id "; 
    
    //Get all appointments, ordered by date and time
    public List<Appointment> getAll() throws SQLException {
        return query(BASE_SELECT + "ORDER BY a.appointment_date, a.appointment_time");
    }
    
    //List for appointments with name matching the keyword in client name, walk-in name, or dentist name
    //Ordered by date and time
    public List<Appointment> search(String keyword) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE u.full_name LIKE ? OR a.walk_in_name LIKE ? OR d.name LIKE ? " +
                "ORDER BY a.appointment_date, a.appointment_time";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }
    
    //List for appointments with name of clients
    private List<Appointment> query(String sql) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }
    
    //Method to create an Appointment object from a ResultSet row, handling nulls for client_id and service_id
    private Appointment mapRow(ResultSet rs) throws SQLException {
        Date d = rs.getDate("appointment_date");
        Time t = rs.getTime("appointment_time");

        int cid = rs.getInt("client_id");
        Integer clientId = rs.wasNull() ? null : cid;

        int svc = rs.getInt("service_id");
        Integer serviceId = rs.wasNull() ? null : svc;

        return new Appointment(
                rs.getInt("id"),
                clientId,
                rs.getString("client_name"),
                rs.getString("walk_in_name"),
                rs.getString("walk_in_phone"),
                rs.getInt("dentist_id"),
                rs.getString("dentist_name"),
                serviceId,
                rs.getString("service_name"),
                d == null ? null : d.toLocalDate(),
                t == null ? null : t.toLocalTime(),
                rs.getString("status"),
                rs.getString("notes"));
    }
    
    //Methods for adding, updating, deleting, and counting appointments
    public boolean insert(Appointment a) throws SQLException {
        String sql = "INSERT INTO appointments " +
                     "(client_id, walk_in_name, walk_in_phone, dentist_id, service_id, " +
                     " appointment_date, appointment_time, status, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (a.getClientId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, a.getClientId());

            if (a.getWalkInName() == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, a.getWalkInName());

            if (a.getWalkInPhone() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, a.getWalkInPhone());

            ps.setInt(4, a.getDentistId());

            if (a.getServiceId() == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, a.getServiceId());

            ps.setDate(6, Date.valueOf(a.getDate()));
            ps.setTime(7, Time.valueOf(a.getTime()));
            ps.setString(8, a.getStatus());
            ps.setString(9, a.getNotes());

            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) a.setId(keys.getInt(1));
                }
                return true;
            }
        }
        return false;
    }
    
    //Update an existing appointment in the database
    public boolean update(Appointment a) throws SQLException {
        String sql = "UPDATE appointments SET client_id=?, walk_in_name=?, walk_in_phone=?, " +
                     "dentist_id=?, service_id=?, appointment_date=?, appointment_time=?, " +
                     "status=?, notes=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (a.getClientId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, a.getClientId());

            if (a.getWalkInName() == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, a.getWalkInName());

            if (a.getWalkInPhone() == null) ps.setNull(3, Types.VARCHAR);
            else ps.setString(3, a.getWalkInPhone());

            ps.setInt(4, a.getDentistId());

            if (a.getServiceId() == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, a.getServiceId());

            ps.setDate(6, Date.valueOf(a.getDate()));
            ps.setTime(7, Time.valueOf(a.getTime()));
            ps.setString(8, a.getStatus());
            ps.setString(9, a.getNotes());
            ps.setInt(10, a.getId());
            return ps.executeUpdate() > 0;
        }
    }
    
    //Update the status of an appointment by its ID
    public boolean updateStatus(int id, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE appointments SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }
    
    //Delete an appointment by its ID
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM appointments WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
    
    //Count all appointments in the database, returning the count
    public int countAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM appointments");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    //Count how many appointments are scheduled for today, returning the count
    public int countToday() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM appointments WHERE appointment_date = CURDATE()");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    //Walk-in client support (used by ManageClientsPanel)
    //Count how many appointments belong to a given walk-in name.
    public int countByWalkInName(String walkInName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments " +
                     "WHERE walk_in_name = ? AND client_id IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, walkInName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    //Delete all appointments for a given walk-in name(For Admin & Staff).
    public int deleteByWalkInName(String walkInName) throws SQLException {
        String sql = "DELETE FROM appointments " +
                     "WHERE walk_in_name = ? AND client_id IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, walkInName);
            return ps.executeUpdate();
        }
    }

    //Delete an appointment ONLY if it belongs to the given client.
    //Prevents a client from removing someone else's appointment by ID.
    public boolean deleteOwnedBy(int appointmentId, int clientId) throws SQLException {
        String sql = "DELETE FROM appointments WHERE id = ? AND client_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            ps.setInt(2, clientId);
            return ps.executeUpdate() > 0;
        }
    }
    
    //Change the status of an appointment ONLY if it belongs to the given client.
    //Used by the client's "Cancel Appointment" action.
    public boolean updateStatusOwnedBy(int appointmentId, int clientId, String status)
            throws SQLException {
        String sql = "UPDATE appointments SET status = ? WHERE id = ? AND client_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            ps.setInt(3, clientId);
            return ps.executeUpdate() > 0;
        }
    }

    //Client Dashboard Specific Operations
    //Get only the appointments for a specific client, ordered newest first.
    //Used by the "My Appointments" panel.
    public List<Appointment> getAppointmentsByClient(int clientId) throws SQLException {
        String sql = BASE_SELECT + "WHERE a.client_id = ? " +
                     "ORDER BY a.appointment_date DESC, a.appointment_time DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    //Count upcoming appointments for the client's home dashboard stat card.
    //Counts appointments that are today or in the future and are Pending/Approved.
    public int countUpcomingByClient(int clientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments " +
                     "WHERE client_id = ? AND appointment_date >= CURDATE() " +
                     "AND status IN ('Pending', 'Approved')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    //Updates the date/time and resets the status to "Pending" so the Admin can review it.
    public boolean requestReschedule(int appointmentId, int clientId, java.time.LocalDate newDate, java.time.LocalTime newTime) throws SQLException {
        String sql = "UPDATE appointments SET appointment_date=?, appointment_time=?, status='Pending' " +
                     "WHERE id = ? AND client_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(newDate));
            ps.setTime(2, Time.valueOf(newTime));
            ps.setInt(3, appointmentId);
            ps.setInt(4, clientId);
            return ps.executeUpdate() > 0;
        }
    }

    //Double-Booking Validation
    //Checks if a dentist is already booked at a specific date and time.
    //Exclude the appointment being edited (excludeId) so it doesn't conflict with itself.
    //Also ignores 'Cancelled' appointments so the slot can be re-booked.
    public boolean hasConflict(int dentistId, LocalDate date, LocalTime time, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments " +
                     "WHERE dentist_id = ? AND appointment_date = ? AND appointment_time = ? " +
                     "AND status != 'Cancelled' ";
        
        //If excludeId is greater than 0, we are editing an existing appointment, 
        //so we should ignore its own ID in the conflict check.
        if (excludeId > 0) {
            sql += "AND id != ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dentistId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            
            if (excludeId > 0) {
                ps.setInt(4, excludeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    //Auto-Cancel Appointments for Inactive Dentists
    //Automatically cancels all Approved appointments for a specific dentist scheduled for TODAY.
    //Used when a dentist's status is changed to "Inactive".
    //Returns the number of appointments cancelled.
    public int cancelTodayApprovedAppointmentsForDentist(int dentistId) throws SQLException {
        String sql = "UPDATE appointments SET status = 'Cancelled' " +
                     "WHERE dentist_id = ? AND status = 'Approved' " +
                     "AND appointment_date = CURDATE()";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dentistId);
            return ps.executeUpdate();
        }
    }

    //METHODS FOR RECENT ACTIVITY FEEDS
    //Fetch the most recent appointments for the Admin Dashboard (across all clients)
    //Ordered by ID descending to get the newest entries first.
    public List<Appointment> getRecentAppointments(int limit) throws SQLException {
        String sql = BASE_SELECT + "ORDER BY a.id DESC LIMIT ?";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    //Fetch the most recent appointments for a specific Client
    //Ordered by ID descending to get the newest entries first.
    public List<Appointment> getRecentAppointmentsByClient(int clientId, int limit) throws SQLException {
        String sql = BASE_SELECT + "WHERE a.client_id = ? ORDER BY a.id DESC LIMIT ?";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }
}