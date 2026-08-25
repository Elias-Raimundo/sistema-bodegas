package bodega_system.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class PreparedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double price;
    private Double costPrice = 0.0;
    
    @OneToMany(mappedBy = "preparedProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreparedProductIngredient> ingredients = new ArrayList<>();

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

    public List<PreparedProductIngredient> getIngredients(){
        return ingredients;
    }

    public Double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Double costPrice) {
        this.costPrice = costPrice;
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

    public void setIngredients(List<PreparedProductIngredient> ingredients){
        this.ingredients = ingredients;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}