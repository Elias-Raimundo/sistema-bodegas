package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.Company;
import bodega_system.entity.TableBar;

public interface TableBarRepository extends JpaRepository<TableBar, Long>{
    List<TableBar> findByCompany(Company company);
}
