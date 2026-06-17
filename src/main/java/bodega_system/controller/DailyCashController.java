package bodega_system.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import bodega_system.dto.CashExpenseDTO;
import bodega_system.dto.CloseDailyCashDTO;
import bodega_system.dto.OpenCashDTO;
import bodega_system.entity.*;
import bodega_system.enums.PaymentMethod;
import bodega_system.repository.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/daily-cash")
public class DailyCashController {

    private final DailyCashRegisterRepository dailyCashRegisterRepository;
    private final CashExpenseRepository cashExpenseRepository;
    private final SaleRepository saleRepository;

    public DailyCashController(
        DailyCashRegisterRepository dailyCashRegisterRepository,
        CashExpenseRepository cashExpenseRepository,
        SaleRepository saleRepository
    ) {
        this.dailyCashRegisterRepository = dailyCashRegisterRepository;
        this.cashExpenseRepository = cashExpenseRepository;
        this.saleRepository = saleRepository;
    }

    @PostMapping("/open")
    public DailyCashRegister openCash(
        @RequestBody OpenCashDTO dto,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        if (dto.openingAmount == null || dto.openingAmount < 0) {
            throw new RuntimeException("El monto inicial no puede ser negativo");
        }

        dailyCashRegisterRepository
            .findByCompany_IdAndStatus(companyId, "OPEN")
            .ifPresent(cash -> {
                throw new RuntimeException("Ya hay una caja abierta");
            });

        Company company = new Company();
        company.setId(companyId);

        DailyCashRegister cash = new DailyCashRegister();

        cash.setCompany(company);
        cash.setOpenedAt(LocalDateTime.now());
        cash.setOpeningAmount(dto.openingAmount);
        cash.setStatus("OPEN");

        return dailyCashRegisterRepository.save(cash);
    }

    @GetMapping("/current")
    public DailyCashRegister currentCash(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        return dailyCashRegisterRepository
            .findByCompany_IdAndStatus(companyId, "OPEN")
            .orElse(null);
    }

    @PostMapping("/expenses")
    public CashExpense addExpense(
        @RequestBody CashExpenseDTO dto,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        if (dto.description == null || dto.description.trim().isEmpty()) {
            throw new RuntimeException("La descripción del gasto es obligatoria");
        }

        if (dto.amount == null || dto.amount <= 0) {
            throw new RuntimeException("El gasto debe ser mayor a cero");
        }

        DailyCashRegister cash = dailyCashRegisterRepository
            .findByCompany_IdAndStatus(companyId, "OPEN")
            .orElseThrow(() -> new RuntimeException("No hay caja abierta"));

        Company company = new Company();
        company.setId(companyId);

        CashExpense expense = new CashExpense();

        expense.setDescription(dto.description.trim());
        expense.setAmount(dto.amount);
        expense.setCreatedAt(LocalDateTime.now());
        expense.setCashRegister(cash);
        expense.setCompany(company);

        return cashExpenseRepository.save(expense);
    }

    @GetMapping("/expenses")
    public List<CashExpense> getExpenses(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        DailyCashRegister cash = dailyCashRegisterRepository
            .findByCompany_IdAndStatus(companyId, "OPEN")
            .orElseThrow(() -> new RuntimeException("No hay caja abierta"));

        return cashExpenseRepository
            .findByCashRegisterOrderByCreatedAtDesc(cash);
    }

    @PostMapping("/close")
    public DailyCashRegister closeCash(
        @RequestBody CloseDailyCashDTO dto,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        if (dto.closingAmount == null || dto.closingAmount < 0) {
            throw new RuntimeException("El monto de cierre no puede ser negativo");
        }

        DailyCashRegister cash = dailyCashRegisterRepository
            .findByCompany_IdAndStatus(companyId, "OPEN")
            .orElseThrow(() -> new RuntimeException("No hay caja abierta"));

        LocalDateTime openedAt = cash.getOpenedAt();
        LocalDateTime closedAt = LocalDateTime.now();

        List<Sale> sales = saleRepository
            .findByCompany_IdOrderByCreatedAtDesc(companyId)
            .stream()
            .filter(s ->
                s.getCreatedAt() != null &&
                !s.getCreatedAt().isBefore(openedAt) &&
                !s.getCreatedAt().isAfter(closedAt)
            )
            .toList();

        double cashSales = 0;

        for (Sale sale : sales) {
            if (sale.getPayments() == null) continue;

            for (SalePayment payment : sale.getPayments()) {
                if (payment.getMethod() == PaymentMethod.CASH) {
                    cashSales += payment.getAmount();
                }
            }
        }

        List<CashExpense> expenses =
            cashExpenseRepository
                .findByCashRegisterOrderByCreatedAtDesc(cash);

        double expensesTotal = expenses.stream()
            .mapToDouble(CashExpense::getAmount)
            .sum();

        double expectedAmount =
            cash.getOpeningAmount()
            + cashSales
            - expensesTotal;

        if (dto.closingAmount > expectedAmount) {
            throw new RuntimeException(
                "El monto que queda no puede ser mayor a la caja teórica"
            );
        }

        double withdrawnAmount =
            expectedAmount - dto.closingAmount;

        cash.setClosedAt(closedAt);
        cash.setClosingAmount(dto.closingAmount);
        cash.setCashSales(cashSales);
        cash.setExpensesTotal(expensesTotal);
        cash.setExpectedAmount(expectedAmount);
        cash.setWithdrawnAmount(withdrawnAmount);
        cash.setDifference(0);
        cash.setStatus("CLOSED");

        return dailyCashRegisterRepository.save(cash);
    }

    @GetMapping("/last-closed")
    public DailyCashRegister lastClosed(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        return dailyCashRegisterRepository
            .findFirstByCompany_IdAndStatusOrderByClosedAtDesc(
                companyId,
                "CLOSED"
            );
    }

    @GetMapping("/{cashId}/expenses")
    public List<CashExpense> getExpensesByCash(
        @PathVariable Long cashId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        DailyCashRegister cash = dailyCashRegisterRepository
            .findById(cashId)
            .orElseThrow();

        if (!cash.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        return cashExpenseRepository
            .findByCashRegister_IdOrderByCreatedAtDesc(cashId);
    }

    @DeleteMapping("/expenses/{expenseId}")
    public void deleteExpense(
        @PathVariable Long expenseId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        CashExpense expense = cashExpenseRepository
            .findById(expenseId)
            .orElseThrow();

        if (!expense.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        cashExpenseRepository.delete(expense);
    }

    @GetMapping("/history")
    public List<DailyCashRegister> history(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        return dailyCashRegisterRepository
            .findByCompany_IdOrderByOpenedAtDesc(companyId);
    }
}