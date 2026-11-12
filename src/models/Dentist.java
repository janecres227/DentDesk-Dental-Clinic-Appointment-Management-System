package models;
public class Dentist {

    //Initialization
    private int id;
    private String name;
    private String specialization;
    private String status; //"Active" or "Inactive"
    
    //Constructor for Dentist class to initialize all the fields
    public Dentist(int id, String name, String specialization, String status) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.status = status;
    }
    //Getters for all the fields for Dentist class
    public int getId() { return id; }
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public String getStatus() { return status; }
    
    //Setters for all the fields for Dentist class
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public void setStatus(String status) { this.status = status; } 

    //Display id like "D001"
    //Method to get the formatted ID of the dentist in "D###" format
    public String getFormattedId() {
        return String.format("D%03d", id);
    }
}
