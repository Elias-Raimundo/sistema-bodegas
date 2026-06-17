package bodega_system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
public class CashExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private double amount;
    private LocalDateTime createdAt;

    @ManyToOne
    private DailyCashRegister cashRegister;

    @ManyToOne
    private Company company;

    public Long getId() { return id; }

    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public DailyCashRegister getCashRegister() { return cashRegister; }
    public Company getCompany() { return company; }

    public void setDescription(String description) { this.description = description; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setCashRegister(DailyCashRegister cashRegister) { this.cashRegister = cashRegister; }
    public void setCompany(Company company) { this.company = company; }
}