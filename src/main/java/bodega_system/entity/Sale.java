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
    private Double discount = 0.0;
    private Double subtotal = 0.0;
    private Double total = 0.0;

    private Long customerId;

    @OneToMany(
        mappedBy = "sale",
        cascade = CascadeType.ALL,
        fetch = FetchType.EAGER
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
    public Double getDiscount(){    return discount;    }
    public Double getSubtotal(){    return subtotal; }
    public Company getCompany() { return company; }
    public List<SalePayment> getPayments(){
        return payments;
    }
    public Long getCustomerId(){    return customerId; }

    public void setId(Long id) { this.id = id; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setTotal(Double total) { this.total = total; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public void setCompany(Company company) { this.company = company; }
    public void setPayments(List<SalePayment> payments) {
        this.payments = payments;
    }
    public void setDiscount(Double discount) {  this.discount = discount;}
    public void setSubtotal(Double subtotal){   this.subtotal =subtotal; }
    public void setCustomerId(Long customerId){ this.customerId = customerId; }
}
