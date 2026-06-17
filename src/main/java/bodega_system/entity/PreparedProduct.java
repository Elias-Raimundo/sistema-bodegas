package bodega_system.entity;

import jakarta.persistence.*;

@Entity
public class PreparedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double price;
    private Double servingsPerUnit;

    @ManyToOne
    @JoinColumn(name = "base_product_id")
    private Product baseProduct;

    @ManyToOne
    private Company company;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public Double getServingsPerUnit() {
        return servingsPerUnit;
    }

    public Product getBaseProduct() {
        return baseProduct;
    }

    public Company getCompany() {
        return company;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setServingsPerUnit(Double servingsPerUnit) {
        this.servingsPerUnit = servingsPerUnit;
    }

    public void setBaseProduct(Product baseProduct) {
        this.baseProduct = baseProduct;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}