package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.Sale;

public interface SaleRepository extends JpaRepository<Sale, Long>{
    List<Sale> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    List<Sale> findByCustomerId(Long customerId);
    
}
