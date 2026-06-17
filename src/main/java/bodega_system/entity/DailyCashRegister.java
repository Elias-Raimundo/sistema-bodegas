package bodega_system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
public class DailyCashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    private double openingAmount;
    private double cashSales;
    private double expensesTotal;
    private double withdrawnAmount;
    private double closingAmount;
    private double expectedAmount;
    private double difference;

    private String status; // OPEN / CLOSED

    @ManyToOne
    private Company company;

    public Long getId() { return id; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }

    public double getOpeningAmount() { return openingAmount; }
    public double getClosingAmount() { return closingAmount; }
    public double getExpectedAmount() { return expectedAmount; }
    public double getDifference() { return difference; }
    public double getCashSales() {  return cashSales; }
    public double getExpensesTotal() {  return expensesTotal; }
    public double getWithdrawnAmount(){ return withdrawnAmount; }

    public String getStatus() { return status; }

    public Company getCompany() { return company; }

    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public void setOpeningAmount(double openingAmount) { this.openingAmount = openingAmount; }
    public void setClosingAmount(double closingAmount) { this.closingAmount = closingAmount; }
    public void setExpectedAmount(double expectedAmount) { this.expectedAmount = expectedAmount; }
    public void setDifference(double difference) { this.difference = difference; }
    public void setCashSales(double cashSales){ this.cashSales = cashSales; }
    public void setExpensesTotal(double expensesTotal){ this.expensesTotal = expensesTotal; }
    public void setWithdrawnAmount(double withdrawnAmount){ this.withdrawnAmount = withdrawnAmount; }

    public void setStatus(String status) { this.status = status; }

    public void setCompany(Company company) { this.company = company; }
}