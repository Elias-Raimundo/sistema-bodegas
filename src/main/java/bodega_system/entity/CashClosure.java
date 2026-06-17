package bodega_system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
public class CashClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private LocalDateTime closedAt;

    private double total;
    private double cash;
    private double transfer;
    private double debit;
    private double credit;

    private Long salesCount;
    private Integer productsSold;

    @ManyToOne
    private Company company;

    public Long getId() { return id; }

    public LocalDateTime getFromDate() { return fromDate; }
    public LocalDateTime getToDate() { return toDate; }
    public LocalDateTime getClosedAt() { return closedAt; }

    public double getTotal() { return total; }
    public double getCash() { return cash; }
    public double getTransfer() { return transfer; }
    public double getDebit() { return debit; }
    public double getCredit() { return credit; }

    public Long getSalesCount() { return salesCount; }
    public Integer getProductsSold() { return productsSold; }

    public Company getCompany() { return company; }

    public void setFromDate(LocalDateTime fromDate) { this.fromDate = fromDate; }
    public void setToDate(LocalDateTime toDate) { this.toDate = toDate; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public void setTotal(double total) { this.total = total; }
    public void setCash(double cash) { this.cash = cash; }
    public void setTransfer(double transfer) { this.transfer = transfer; }
    public void setDebit(double debit) { this.debit = debit; }
    public void setCredit(double credit) { this.credit = credit; }

    public void setSalesCount(Long salesCount) { this.salesCount = salesCount; }
    public void setProductsSold(Integer productsSold) { this.productsSold = productsSold; }

    public void setCompany(Company company) { this.company = company; }
}
