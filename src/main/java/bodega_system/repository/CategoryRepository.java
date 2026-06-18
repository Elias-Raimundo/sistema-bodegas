package bodega_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.Category;
import bodega_system.entity.Company;

public interface CategoryRepository extends JpaRepository<Category, Long>{
    boolean existsByNameAndCompany( String name, Company company);

    List<Category> findByCompany(Company company);

    Optional<Category> findByIdAndCompany(Long id, Company company);

    Optional<Category> findByNameIgnoreCaseAndCompany(String name, Company company);
}
