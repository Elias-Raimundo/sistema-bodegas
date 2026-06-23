package bodega_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;


@Entity
public class Supplier{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @ManyToOne
    private Company company;

    public void setName(String name){   this.name = name;}
    public void setDescription(String description){ this.description = description;}
    public void setCompany(Company company){ this.company = company; }
    public void setId(Long id){ this.id = id; }
    public Long getId() { return id; }
    public String getName(){    return name;}
    public String getDescription() {    return description; }
    public Company getCompany() {   return company; }
}