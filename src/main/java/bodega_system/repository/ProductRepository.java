package bodega_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>{
    List<Product> findByCompanyId(Long companyId);

    List<Product> findByCompanyIdAndCategoryId(Long companyId, Long categoryId);

    List<Product> findByCategoryId(Long categoryId);
}
