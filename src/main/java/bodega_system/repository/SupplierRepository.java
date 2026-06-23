package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByCompany_IdOrderByNameAsc(Long companyId);
}
