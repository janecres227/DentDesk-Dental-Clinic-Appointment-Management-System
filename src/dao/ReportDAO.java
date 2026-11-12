package dao;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import models.ReportRow;
import models.ReportSummary;

public class ReportDAO {

    //Get the Summary Stats for the top cards
    public ReportSummary getSummary(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT " +
                     "COUNT(a.id) AS total, " +
                     "SUM(CASE WHEN a.status IN ('Approved', 'Completed') THEN 1 ELSE 0 END) AS success, " +
                     "SUM(CASE WHEN a.status = 'Cancelled' THEN 1 ELSE 0 END) AS cancelled, " +
                     "SUM(CASE WHEN a.status IN ('Approved', 'Completed') THEN COALESCE(s.price, 0) ELSE 0 END) AS income " +
                     "FROM appointments a " +
                     "LEFT JOIN services s ON a.service_id = s.id " +
                     "WHERE a.appointment_date BETWEEN ? AND ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ReportSummary(
                            rs.getInt("total"),
                            rs.getInt("success"),
                            rs.getInt("cancelled"),
                            rs.getDouble("income")
                    );
                }
            }
        }
        return new ReportSummary(0, 0, 0, 0);
    }

    //Get the detailed rows for the table(supports sorting)
    public List<ReportRow> getReportData(LocalDate from, LocalDate to, String sortBy) throws SQLException {
        //Determine Order By clause based on user selection
        String orderBy = switch (sortBy == null ? "" : sortBy) {
         case "Highest Price" -> "ORDER BY s.price DESC";
         case "Lowest Price" -> "ORDER BY s.price ASC";
         case "Oldest First" -> "ORDER BY a.appointment_date ASC";
         default -> "ORDER BY a.appointment_date DESC";
};
        //Build the SQL query to fetch appointment reports.
        //The query joins the appointments table with users (clients), dentists, and services.
        //COALESCE(u.full_name, a.walk_in_name) ensures that if a client doesn't have an account (i.e., it's a walk-in), it grabs the walk-in name instead.
        //COALESCE(s.price, 0) ensures that if a service was deleted or not specified, the price defaults to 0.
        //The orderBy variable is appended at the end to allow dynamic sorting (e.g., ORDER BY a.appointment_date DESC).
        String sql = "SELECT a.id, a.appointment_date, COALESCE(u.full_name, a.walk_in_name) AS client_name, " +
                     "d.name AS dentist_name, s.services_name AS service_name, COALESCE(s.price, 0) AS price, a.status " +
                     "FROM appointments a " +
                     "LEFT JOIN users u ON a.client_id = u.id " +
                     "JOIN dentists d ON a.dentist_id = d.id " +
                     "LEFT JOIN services s ON a.service_id = s.id " +
                     "WHERE a.appointment_date BETWEEN ? AND ? " +
                     orderBy;
        
        List<ReportRow> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReportRow(
                            rs.getInt("id"),
                            rs.getDate("appointment_date").toLocalDate(),
                            rs.getString("client_name"),
                            rs.getString("dentist_name"),
                            rs.getString("service_name"),
                            rs.getDouble("price"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }
}
