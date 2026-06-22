package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCompany_IdOrderByNameAsc(Long companyId);
}