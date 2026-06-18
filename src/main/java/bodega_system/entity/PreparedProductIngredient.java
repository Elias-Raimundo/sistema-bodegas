package bodega_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class PreparedProductIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double quantity;

    @ManyToOne
    @JoinColumn(name = "prepared_product_id")
    @JsonIgnore
    private PreparedProduct preparedProduct;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public Long getId() {
        return id;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public PreparedProduct getPreparedProduct() {
        return preparedProduct;
    }

    public void setPreparedProduct(PreparedProduct preparedProduct) {
        this.preparedProduct = preparedProduct;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}