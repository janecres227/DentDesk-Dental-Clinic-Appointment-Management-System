package models;
import java.time.LocalDate;

//Model class representing a single row of data in the Admin's Financial/Appointment Report
public class ReportRow {
    
    //Initialization
    final private int appointmentId;
    final private LocalDate date;
    final private String clientName;
    final private String dentistName;
    final private String serviceName;
    final private double price;
    final private String status;

    //Constructor for ReportRow with all necessary details for the report
    public ReportRow(int appointmentId, LocalDate date, String clientName, String dentistName, String serviceName, double price, String status) {
        this.appointmentId = appointmentId;
        this.date = date;
        this.clientName = clientName;
        this.dentistName = dentistName;
        this.serviceName = serviceName;
        this.price = price;
        this.status = status;
    }
    
    //Getters
    public int getAppointmentId() { return appointmentId; }
    public LocalDate getDate() { return date; }
    public String getClientName() { return clientName; }
    public String getDentistName() { return dentistName; }
    public String getServiceName() { return serviceName; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }
}
