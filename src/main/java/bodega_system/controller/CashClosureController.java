package bodega_system.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import bodega_system.entity.*;
import bodega_system.repository.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/cash-closures")
public class CashClosureController {

    private final CashClosureRepository cashClosureRepository;
    private final SaleRepository saleRepository;

    public CashClosureController(
        CashClosureRepository cashClosureRepository,
        SaleRepository saleRepository
    ) {
        this.cashClosureRepository = cashClosureRepository;
        this.saleRepository = saleRepository;
    }

    @PostMapping
    public CashClosure create(
        @RequestParam String from,
        @RequestParam String to,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Company company = new Company();
        company.setId(companyId);

        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);

        if (fromDate.isAfter(toDate)){
            throw new RuntimeException("La fecha desde no puede ser mayor a la fecha hasta");
        }
        if (fromDate.equals(toDate)){
            throw new RuntimeException("El priodo no puede ser vacio");
        }

        List<Sale> sales = saleRepository
            .findByCompany_IdOrderByCreatedAtDesc(companyId)
            .stream()
            .filter(s ->
                s.getCreatedAt() != null &&
                !s.getCreatedAt().isBefore(fromDate) &&
                !s.getCreatedAt().isAfter(toDate)
            )
            .toList();
        
        if (sales.isEmpty()){
            throw new RuntimeException("No hay ventas ene el periodo seleccionado");
        }

        CashClosure closure = new CashClosure();

        closure.setCompany(company);
        closure.setFromDate(fromDate);
        closure.setToDate(toDate);
        closure.setClosedAt(LocalDateTime.now());

        closure.setTotal(
            sales.stream()
                .mapToDouble(Sale::getTotal)
                .sum()
        );

        closure.setSalesCount((long) sales.size());

        closure.setProductsSold(
            sales.stream()
                .flatMap(s -> s.getItems().stream())
                .mapToInt(SaleItem::getQuantity)
                .sum()
        );

        double cash = 0;
        double transfer = 0;
        double debit = 0;
        double credit = 0;

        for (Sale sale : sales) {

            if (sale.getPayments() == null) continue;

            for (SalePayment payment : sale.getPayments()) {

                switch (payment.getMethod()) {

                    case CASH ->
                        cash += payment.getAmount();

                    case TRANSFER ->
                        transfer += payment.getAmount();

                    case DEBIT ->
                        debit += payment.getAmount();

                    case CREDIT ->
                        credit += payment.getAmount();
                }
            }
        }

        closure.setCash(cash);
        closure.setTransfer(transfer);
        closure.setDebit(debit);
        closure.setCredit(credit);

        return cashClosureRepository.save(closure);
    }

    @GetMapping
    public List<CashClosure> getAll(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        return cashClosureRepository
            .findByCompany_IdOrderByClosedAtDesc(companyId);
    }
}