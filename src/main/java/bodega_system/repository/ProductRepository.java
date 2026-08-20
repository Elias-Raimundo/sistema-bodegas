package bodega_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.Product;
import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>{
    List<Product> findByCompanyId(Long companyId);

    List<Product> findByCompanyIdAndCategoryId(Long companyId, Long categoryId);

    List<Product> findByCategoryId(Long categoryId);

    Optional<Product> findByNameIgnoreCaseAndCompanyId(String name, Long companyId);

    List<Product> findByCompany_IdAndNameContainingIgnoreCase(
        Long companyId, String name
    );

    List<Product> findByCompany_IdAndNameContainingIgnoreCaseAndCategory_Id(
        Long companyId, String name, Long categoryId
    );

    // ---- NUEVO: solo para el dashboard, no toca lo existente ----
    List<Product> findByCompanyIdAndStockLessThanOrderByStockAsc(
        Long companyId, Double stock, org.springframework.data.domain.Pageable pageable
    );

    long countByCompanyIdAndStockLessThan(Long companyId, Double stock);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(p), COALESCE(SUM(p.stock),0), COALESCE(SUM(p.price * FLOOR(p.stock)),0) " +
        "FROM Product p WHERE p.company.id = :companyId"
    )
    Object[] getInventorySummary(@org.springframework.data.repository.query.Param("companyId") Long companyId);
}
