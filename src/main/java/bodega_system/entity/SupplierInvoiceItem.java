package bodega_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

// Renglón de una compra a proveedor: qué producto, cuánto y a qué costo
@Entity
public class SupplierInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;
    private Double quantity;
    private Double unitCost;

    // Precio de venta que quedó después de la compra (null = no se cambió)
    private Double salePrice;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private SupplierInvoice invoice;

    public Long getId() { return id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }

    public Double getSalePrice() { return salePrice; }
    public void setSalePrice(Double salePrice) { this.salePrice = salePrice; }

    public SupplierInvoice getInvoice() { return invoice; }
    public void setInvoice(SupplierInvoice invoice) { this.invoice = invoice; }
}
