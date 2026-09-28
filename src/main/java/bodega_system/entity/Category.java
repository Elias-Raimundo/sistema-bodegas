package bodega_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
public class Category {
    
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "company_id")
    @JsonIgnore // evita mandar la empresa (con el logo) en cada respuesta
    private Company company;

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public Long getId(){
        return id;
    }

    public Company getCompany(){
        return company;
    }

    public void setCompany(Company company){
        this.company = company;
    }
}
