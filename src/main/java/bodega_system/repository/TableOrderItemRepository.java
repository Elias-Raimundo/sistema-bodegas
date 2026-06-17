package bodega_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.TableOrderItem;

public interface TableOrderItemRepository extends JpaRepository<TableOrderItem, Long>{
    
}
