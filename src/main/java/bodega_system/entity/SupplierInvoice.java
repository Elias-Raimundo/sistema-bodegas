package bodega_system.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
public class SupplierInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    private Double totalAmount;

    private Double paidAmount = 0.0;

    private String description;

    // Detalle de productos (solo en facturas cargadas como compra)
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    @BatchSize(size = 50)
    private List<SupplierInvoiceItem> items = new ArrayList<>();

    @ManyToOne
    private Supplier supplier;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Double getPendingAmount() {
        return (totalAmount == null ? 0 : totalAmount)
             - (paidAmount == null ? 0 : paidAmount);
    }

    public String getStatus() {

        double pending = getPendingAmount();

        if (pending <= 0) {
            return "PAGADA";
        }

        if (paidAmount != null && paidAmount > 0) {
            return "PARCIAL";
        }

        return "PENDIENTE";
    }

    public List<SupplierInvoiceItem> getItems() { return items; }
    public void setItems(List<SupplierInvoiceItem> items) { this.items = items; }
}
