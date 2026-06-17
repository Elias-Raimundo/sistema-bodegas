package bodega_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class TableBar{

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean occupied;

    @ManyToOne
    private Company company;

    public Company getCompany(){
        return company;
    }
    public String getName(){
        return name;
    }
    public boolean getOccupied(){
        return occupied;
    }
    public Long getId(){
        return id;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setOccupied(boolean occupied){
        this.occupied = occupied;
    }

    public void setCompany(Company company){
        this.company = company;
    }
    public void setId(Long id){
        this.id = id;
    }

    public boolean isOccupied(){
        return occupied;
    }
}