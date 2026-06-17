package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.Company;
import bodega_system.entity.PreparedProduct;

public interface PreparedProductRepository extends JpaRepository<PreparedProduct, Long>{
    List<PreparedProduct> findByCompany(Company company);
}
