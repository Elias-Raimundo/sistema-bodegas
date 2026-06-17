package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.CashExpense;
import bodega_system.entity.DailyCashRegister;

public interface CashExpenseRepository
    extends JpaRepository<CashExpense, Long> {

    List<CashExpense> findByCashRegisterOrderByCreatedAtDesc(
        DailyCashRegister cashRegister
    );

    List<CashExpense> findByCashRegister_IdOrderByCreatedAtDesc(
        Long cashRegisterId
    );
}