package models;
public class Service {
    private int id;
    private String servicesName;
    private String description;
    private double price;
    
    //Constructor for Services class to initialize all the fields
    public Service(int id, String servicesName, String description, double price) {
        this.id = id;
        this.servicesName = servicesName;
        this.description = description;
        this.price = price;
    }
    
    //Getters for all the fields for Services class
    public int getId() { return id; }
    public String getServicesName() { return servicesName; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }

    //Setters for all the fields for Services class
    public void setId(int id) { this.id = id; }
    public void setServicesName(String servicesName) { this.servicesName = servicesName; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    
    //Override the toString method to return the servicesName when the object is printed
    @Override
    public String toString() { return servicesName; }
}