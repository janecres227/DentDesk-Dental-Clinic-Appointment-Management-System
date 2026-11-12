package models;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Schedule {
    private int id; 
    private int dentistId;
    private String dentistName;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String availability;//"Available" | "Fully Booked" | "Day Off"
    
    //Constructor for Schedule class to initialize all the fields
    public Schedule(int id, int dentistId, String dentistName,
                    String dayOfWeek, LocalTime startTime, LocalTime endTime,
                    String availability) {
        this.id = id;
        this.dentistId = dentistId;
        this.dentistName = dentistName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.availability = availability;
    }
    
    //Getters for all the fields for Schedule class
    public int getId() { return id; }
    public int getDentistId() { return dentistId; }
    public String getDentistName() { return dentistName; }
    public String getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getAvailability() { return availability; }
    
    //Setters for all the fields for Schedule class
    public void setId(int id) { this.id = id; }
    public void setDentistId(int dentistId) { this.dentistId = dentistId; }
    public void setDentistName(String dentistName) { this.dentistName = dentistName; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setAvailability(String availability) { this.availability = availability; }
    
    //Method to get the formatted ID of the schedule in "S###" format
    public String getFormattedId() {
        return String.format("S%03d", id);
    }
    
    //Method to get the formatted time slot in "hh:mm a - hh:mm a" format
    public String getFormattedTimeSlot() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("hh:mm a");
        return startTime.format(f) + " - " + endTime.format(f);
    }
}
