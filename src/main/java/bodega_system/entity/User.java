package bodega_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;

    private String resetCode;
    private Long resetCodeExpiry;
    private Integer resetAttempts;

    private String avatar;

    @ManyToOne
    @JoinColumn(name = "company_id")
    @JsonIgnore // evita mandar la empresa (con el logo) en cada respuesta
    private Company company;

    public Long getId(){
        return id;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public Company getCompany(){
        return company;
    }

    public void setCompany(Company company){
        this.company = company;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public Long getResetCodeExpiry() {
        return resetCodeExpiry;
    }

    public void setResetCodeExpiry(Long resetCodeExpiry) {
        this.resetCodeExpiry = resetCodeExpiry;
    }

    public Integer getResetAttempts() {
        return resetAttempts;
    }

    public void setResetAttempts(Integer resetAttempts) {
        this.resetAttempts = resetAttempts;
    }
}
