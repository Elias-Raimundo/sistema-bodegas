package bodega_system.entity;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

@Entity
public class Sale {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    private double total;

    @OneToMany(
        mappedBy = "sale",
        cascade = CascadeType.ALL
    )
    private List<SalePayment> payments;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<SaleItem> items;

    
    @ManyToOne
    private Company company;

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Double getTotal() { return total; }
    public List<SaleItem> getItems() { return items; }
    public Company getCompany() { return company; }
    public List<SalePayment> getPayments(){
        return payments;
    }

    public void setId(Long id) { this.id = id; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setTotal(Double total) { this.total = total; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public void setCompany(Company company) { this.company = company; }
    public void setPayments(List<SalePayment> payments) {
        this.payments = payments;
    }
}
