package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.TableOrderItem;

public interface TableOrderItemRepository extends JpaRepository<TableOrderItem, Long>{
    List<TableOrderItem> findByLinkedGroupId(String linkedGroupId);
}
