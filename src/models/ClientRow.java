package models;
public class ClientRow {
    
    //Initialization
    public enum Type { REGISTERED, WALK_IN } //Enum to differentiate between registered clients and walk-ins
    private final Type type;
    private final Integer userId; //null for walk-in
    private final String name;
    private final String phone; 
    private final String email; 
    private final String status;
    
    //Constructor for ClientRow class to initialize all the fields
    public ClientRow(Type type, Integer userId, String name,
                     String phone, String email, String status) {
        this.type = type;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.status = status;
    }
    
    //Getters for all the fields for ClientRow class
    public Type getType() { return type; }
    public Integer getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getStatus() { return status; } 
}
