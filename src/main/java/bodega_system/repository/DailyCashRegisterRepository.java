package bodega_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.DailyCashRegister;

public interface DailyCashRegisterRepository
    extends JpaRepository<DailyCashRegister, Long> {

    Optional<DailyCashRegister> findByCompany_IdAndStatus(
        Long companyId,
        String status
    );

    List<DailyCashRegister> findByCompany_IdOrderByOpenedAtDesc(
        Long companyId
    );

    DailyCashRegister findFirstByCompany_IdAndStatusOrderByClosedAtDesc(
        Long companyId,
        String status
    );
}