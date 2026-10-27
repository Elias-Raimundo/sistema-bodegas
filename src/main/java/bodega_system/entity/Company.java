package bodega_system.entity;

import jakarta.persistence.*;

@Entity
public class Company {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    private String primaryColor;
    private Boolean darkMode;


    // El logo NO se mapea acá a propósito: la columna "logo" sigue en la
    // tabla, pero se lee/escribe con CompanyRepository.findLogoById /
    // updateLogo. Así no viaja en cada consulta que carga la empresa.


    public Long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }


    public String getPrimaryColor(){
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor){
        this.primaryColor = primaryColor;
    }

    public Boolean getDarkMode(){
        return darkMode;
    }

    public void setDarkMode(Boolean darkMode){
        this.darkMode = darkMode;
    }

    public void setId(Long id){
        this.id = id;
    }
}
