package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import models.Schedule;

public class ScheduleDAO {
    
    //Base SQL query to select schedule along with dentist information.
    private static final String BASE_SELECT =
        "SELECT s.id, s.dentist_id, d.name AS dentist_name, " +
        "       s.day_of_week, s.start_time, s.end_time, s.availability " +
        "FROM schedules s " +
        "JOIN dentists d ON d.id = s.dentist_id ";
    
    //SQL Query to order by day of week and start time.
    private static final String ORDER_BY =
        "ORDER BY FIELD(s.day_of_week, " +
        "  'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'), " +
        "  s.start_time";
    
    //Get all schedules, ordered by day of week and start time.
    public List<Schedule> getAll() throws SQLException {
        return query(BASE_SELECT + ORDER_BY, null);
    }

    //Returns a list of schedules matching the search keyword in dentist name, day of week, or availability.
    public List<Schedule> search(String keyword) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE d.name LIKE ? OR s.day_of_week LIKE ? OR s.availability LIKE ? " +
                ORDER_BY;
        List<Schedule> list = new ArrayList<>();
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

    //Returns schedule list from ResultSet, mapping each row to a Schedule object.
    private List<Schedule> query(String sql, String unused) throws SQLException {
        List<Schedule> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    //Method to map a ResultSet row to a Schedule object, converting SQL Time to LocalTime.
    //Display the dentist information and schedule details from start time to end time, along with availability status.
    private Schedule mapRow(ResultSet rs) throws SQLException {
        Time start = rs.getTime("start_time");
        Time end = rs.getTime("end_time");
        return new Schedule(
                rs.getInt("id"),
                rs.getInt("dentist_id"),
                rs.getString("dentist_name"),
                rs.getString("day_of_week"),
                start == null ? null : start.toLocalTime(),
                end == null ? null : end.toLocalTime(),
                rs.getString("availability"));
    }
    
    //Add a new dentist schedule to the database. If successful, set the generated ID in the Schedule object.
    public boolean insert(Schedule s) throws SQLException {
        String sql = "INSERT INTO schedules " +
                     "(dentist_id, day_of_week, start_time, end_time, availability) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getDentistId());
            ps.setString(2, s.getDayOfWeek());
            ps.setTime(3, Time.valueOf(s.getStartTime()));
            ps.setTime(4, Time.valueOf(s.getEndTime()));
            ps.setString(5, s.getAvailability());
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) s.setId(keys.getInt(1));
                }
                return true;
            }
        }
        return false;
    }

    //Update an existing dentist schedule in the database.
    public boolean update(Schedule s) throws SQLException {
        String sql = "UPDATE schedules SET dentist_id=?, day_of_week=?, " +
                     "start_time=?, end_time=?, availability=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getDentistId());
            ps.setString(2, s.getDayOfWeek());
            ps.setTime(3, Time.valueOf(s.getStartTime()));
            ps.setTime(4, Time.valueOf(s.getEndTime()));
            ps.setString(5, s.getAvailability());
            ps.setInt(6, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    //Delete a dentist schedule from the database.
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM schedules WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    //Count the total number of schedules in the database.
    public int countAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM schedules");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    //Validation for Duplicate Schedules
    //Checks if a dentist already has a schedule on a specific day.
    public boolean hasConflict(int dentistId, String dayOfWeek, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM schedules WHERE dentist_id = ? AND day_of_week = ?";
        
        //If excludeId is greater than 0, we are editing an existing schedule,
        //so we should ignore its own ID in the conflict check.
        if (excludeId > 0) {
            sql += " AND id != ?";
        }
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dentistId);
            ps.setString(2, dayOfWeek);
            
            if (excludeId > 0) {
                ps.setInt(3, excludeId);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}