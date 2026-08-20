package bodega_system.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import bodega_system.entity.Sale;

public interface SaleRepository extends JpaRepository<Sale, Long>{

    List<Sale> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    List<Sale> findByCustomerId(Long customerId);

    List<Sale> findByCompany_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long companyId,
        LocalDateTime from,
        LocalDateTime to
    );

    // ---- NUEVO: paginado, solo para el dashboard ----
    List<Sale> findByCompany_IdOrderByCreatedAtDesc(
        Long companyId,
        org.springframework.data.domain.Pageable pageable
    );

    // ---- NUEVO: para /sales/stats, suma en la DB en vez de en Java ----
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.company.id = :companyId AND s.createdAt >= :from"
    )
    double sumTotalSince(
        @org.springframework.data.repository.query.Param("companyId") Long companyId,
        @org.springframework.data.repository.query.Param("from") LocalDateTime from
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.company.id = :companyId"
    )
    double sumTotalAll(@org.springframework.data.repository.query.Param("companyId") Long companyId);


    @org.springframework.data.jpa.repository.Query(
        "SELECT sp.method, COALESCE(SUM(sp.amount), 0) " +
        "FROM Sale s JOIN s.payments sp " +
        "WHERE s.company.id = :companyId " +
        "GROUP BY sp.method"
    )
    List<Object[]> sumPaymentsByMethod(
        @org.springframework.data.repository.query.Param("companyId") Long companyId
    );
}
