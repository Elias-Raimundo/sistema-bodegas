package bodega_system.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class SaleItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemType; // PRODUCT o PREPARED

    private Long productId;
    private Long preparedProductId;

    private String productName;
    private int quantity;
    private double price;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    @JsonBackReference
    private Sale sale;

    public Long getId() { return id; }

    public String getItemType() { return itemType; }
    public Long getProductId() { return productId; }
    public Long getPreparedProductId() { return preparedProductId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public Sale getSale(){ return sale; }

    public void setId(Long id) { this.id = id; }

    public void setItemType(String itemType) { this.itemType = itemType; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setPreparedProductId(Long preparedProductId) { this.preparedProductId = preparedProductId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPrice(double price) { this.price = price; }
    public void setSale(Sale sale) { this.sale = sale; }
}