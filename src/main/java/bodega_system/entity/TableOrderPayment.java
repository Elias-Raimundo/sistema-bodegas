package bodega_system.entity;

import java.time.LocalDateTime;
import bodega_system.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class TableOrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private Double amount;

    private Long customerId; // solo se usa si method = CURRENT_ACCOUNT

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private TableOrder order;

    public Long getId() { return id; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public TableOrder getOrder() { return order; }
    public void setOrder(TableOrder order) { this.order = order; }
}