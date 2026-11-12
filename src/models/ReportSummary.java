package models;

//Represents a statistical summary of appointment metrics and financial data for a specific date range.
public class ReportSummary {
    public int totalAppointments; //Total number of scheduled appointments  
    public int successfulAppointments; //Number of successfully completed appointments 
    public int cancelledAppointments; //Number of cancelled appointments 
    public double totalIncome; //Financial income from completed appointments

    //Constructor for ReportSummary object with pre-calculated report metrics
    public ReportSummary(int totalAppointments, int successfulAppointments, int cancelledAppointments, double totalIncome) {
        this.totalAppointments = totalAppointments;
        this.successfulAppointments = successfulAppointments;
        this.cancelledAppointments = cancelledAppointments;
        this.totalIncome = totalIncome;
    }
}
