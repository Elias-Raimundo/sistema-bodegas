package bodega_system.repository;

import bodega_system.entity.TableOrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableOrderPaymentRepository extends JpaRepository<TableOrderPayment, Long> {
}
