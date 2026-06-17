package bodega_system.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.TableBar;
import bodega_system.entity.TableOrder;

public interface TableOrderRepository extends JpaRepository<TableOrder, Long>{
    Optional<TableOrder> findByTableAndClosedFalse(TableBar table);
    List<TableOrder> findByTable(TableBar table);
}
