package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.CashClosure;

public interface CashClosureRepository extends JpaRepository<CashClosure, Long> {

    List<CashClosure> findByCompany_IdOrderByClosedAtDesc(Long companyId);
}