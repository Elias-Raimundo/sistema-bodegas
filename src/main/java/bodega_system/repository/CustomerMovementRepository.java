package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.CustomerMovement;

public interface CustomerMovementRepository extends JpaRepository<CustomerMovement, Long> {

    List<CustomerMovement> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);
}