package models;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Appointment {
    //Initialization of all the fields for Appointment class
    private int id;
    private Integer clientId;
    private String clientName;
    private String walkInName;
    private String walkInPhone;      
    private int dentistId;
    private String dentistName;
    private Integer serviceId;
    private String serviceName;
    private LocalDate date;
    private LocalTime time;
    private String status;
    private String notes;

    //Constructor for walk-in appointments
    public Appointment(int id, Integer clientId, String clientName,
                       String walkInName, String walkInPhone,
                       int dentistId, String dentistName,
                       Integer serviceId, String serviceName,
                       LocalDate date, LocalTime time,
                       String status, String notes) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.walkInName = walkInName;
        this.walkInPhone = walkInPhone;
        this.dentistId = dentistId;
        this.dentistName = dentistName;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.notes = notes;
    } 
    
    //Getters for all the fields for Appointment class
    public int getId() { return id; }
    public Integer getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getWalkInName() { return walkInName; }
    public String getWalkInPhone() { return walkInPhone; }
    public int getDentistId() { return dentistId; }
    public String getDentistName() { return dentistName; }
    public Integer getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }

    //Setters for all the fields for Appointment class
    public void setId(int id) { this.id = id; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setWalkInName(String walkInName) { this.walkInName = walkInName; }
    public void setWalkInPhone(String walkInPhone) { this.walkInPhone = walkInPhone; }
    public void setDentistId(int dentistId) { this.dentistId = dentistId; }
    public void setDentistName(String dentistName) { this.dentistName = dentistName; }
    public void setServiceId(Integer serviceId) { this.serviceId = serviceId; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(LocalTime time) { this.time = time; }
    public void setStatus(String status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getFormattedId() { return String.format("A%03d", id); } //Method to get the formatted ID of the appointment in "A###" format

    //Method to get the formatted date and time of the appointment in "MM/dd/yyyy hh:mm a" format
    public String getFormattedDateTime() {
        if (date == null || time == null) return "";
        return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                + " " + time.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }
}