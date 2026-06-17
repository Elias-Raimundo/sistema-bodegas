package bodega_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long>{
    

}
