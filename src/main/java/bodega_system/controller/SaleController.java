package bodega_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import bodega_system.dto.SalesReportDTO;
import bodega_system.dto.SalesStatsDTO;
import bodega_system.entity.*;
import bodega_system.repository.*;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/sales")
public class SaleController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final PreparedProductRepository preparedProductRepository;
    private final CustomerRepository customerRepository;
    private final CustomerMovementRepository customerMovementRepository;

    public SaleController(SaleRepository saleRepository, 
        ProductRepository productRepository, 
        PreparedProductRepository preparedProductRepository,
        CustomerRepository customerRepository,
        CustomerMovementRepository customerMovementRepository
    ) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.preparedProductRepository = preparedProductRepository;
        this.customerMovementRepository = customerMovementRepository;
        this.customerRepository = customerRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Sale sale, HttpServletRequest req) {

        Long companyId = (Long) req.getAttribute("companyId");

        Company company = new Company();
        company.setId(companyId);

        double subtotal = 0;

        sale.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires")));
        System.out.println("COMPANY ID: " + companyId);
        sale.setCompany(company);

        if (sale.getItems() == null || sale.getItems().isEmpty()){
            throw new RuntimeException("Debe agregar al menos un producto");
        }

        for (SaleItem item : sale.getItems()) {
            if (item.getQuantity() <= 0){
                throw new RuntimeException("La cantidad debe ser mayor a cero");
            }
            String itemType = item.getItemType();
            if (itemType == null){
                itemType = "PRODUCT";
            }

            if (itemType.equals("PRODUCT")){
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow();
                if (!product.getCompany().getId().equals(companyId)){
                    throw new RuntimeException("Producto no autorizado");
                }
                if (product.getStock() < item.getQuantity()){
                    throw new RuntimeException(
                        "Stock insuficiente para " + product.getName()
                    );
                }

                product.setStock(product.getStock() - item.getQuantity());
                productRepository.save(product);

                item.setProductName(product.getName());
                item.setPrice(product.getPrice());
                item.setItemType("PRODUCT");

                subtotal += item.getQuantity() * product.getPrice();
            }
            if (itemType.equals("PREPARED")) {
                PreparedProduct prepared =
                        preparedProductRepository
                                .findById(item.getPreparedProductId())
                                .orElseThrow();

                if (!prepared.getCompany().getId().equals(companyId)) {
                    throw new RuntimeException("Preparado no autorizado");
                }

                if (prepared.getIngredients() == null || prepared.getIngredients().isEmpty()) {
                    throw new RuntimeException("El preparado no tiene ingredientes");
                }

                for (PreparedProductIngredient ingredient : prepared.getIngredients()) {

                    Product product = ingredient.getProduct();

                    if (!product.getCompany().getId().equals(companyId)) {
                        throw new RuntimeException("Ingrediente no autorizado");
                    }

                    double stockToDiscount =
                            ingredient.getQuantity() * item.getQuantity();

                    if (product.getStock() < stockToDiscount) {
                        throw new RuntimeException(
                            "Stock insuficiente para " + product.getName()
                        );
                    }
                }

                for (PreparedProductIngredient ingredient : prepared.getIngredients()) {

                    Product product = ingredient.getProduct();

                    double stockToDiscount =
                            ingredient.getQuantity() * item.getQuantity();

                    product.setStock(product.getStock() - stockToDiscount);
                    productRepository.save(product);
                }

                item.setProductName(prepared.getName());
                item.setPrice(prepared.getPrice());
                item.setItemType("PREPARED");

                subtotal += item.getQuantity() * prepared.getPrice();
            }

            item.setSale(sale);
        }

        sale.setSubtotal(subtotal);
        double discount =
            sale.getDiscount() == null
                ? 0
                : sale.getDiscount();
        if (discount < 0){
            throw new RuntimeException("El descuento no puede ser negativo");
        }
        if (discount > subtotal){
            throw new RuntimeException("El descuento no puede ser mayor al subtotal");
        }

        double total = subtotal - discount;
        sale.setDiscount(discount);
        sale.setTotal(total);
        
        if (sale.getPayments() == null || sale.getPayments().isEmpty()){
            throw new RuntimeException("Debe registrar al menos un pago");
        }
        for(SalePayment payment: sale.getPayments()){
            if (payment.getAmount() <= 0){
                throw new RuntimeException("El importe del pago debe ser mayor a cero");
            }
        }

        boolean isCurrentAccount = sale.getPayments()
            .stream()
            .anyMatch(payment ->
                payment.getMethod() != null &&
                payment.getMethod().name().equals("CURRENT_ACCOUNT")
            );
        if (isCurrentAccount && sale.getCustomerId() == null){
            throw new RuntimeException("Debe seleccionar un cliente para cuenta corriente");
        }

        double paymentsTotal = sale.getPayments()
            .stream()
            .mapToDouble(SalePayment::getAmount)
            .sum();

        if (Math.abs(paymentsTotal - total) > 0.01) {
            throw new RuntimeException("Los pagos no coinciden con el total");
        }

        for (SalePayment payment : sale.getPayments()) {
            payment.setSale(sale);
        }

        Sale savedSale = saleRepository.save(sale);

        if (savedSale.getId() == null) {
            throw new RuntimeException("La venta no se realizo");
        }

        if (isCurrentAccount) {
            Customer customer = customerRepository.findById(sale.getCustomerId())
                .orElseThrow();
            if (!customer.getCompany().getId().equals(companyId)) {
                throw new RuntimeException("Cliente no autorizado");
            }

            double currentAccountAmount = sale.getPayments()
                .stream()
                .filter(p -> p.getMethod().name().equals("CURRENT_ACCOUNT"))
                .mapToDouble(SalePayment::getAmount)
                .sum();

            CustomerMovement movement = new CustomerMovement();
            movement.setCustomer(customer);
            movement.setSaleId(savedSale.getId());
            movement.setType("DEBT");
            movement.setAmount(currentAccountAmount);
            movement.setDescription("Venta #" + savedSale.getId());
            movement.setCreatedAt(LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")));

            customerMovementRepository.save(movement);
            customer.setBalance(
                (customer.getBalance() == null ? 0 : customer.getBalance())
                + currentAccountAmount
            );
            customerRepository.save(customer);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedSale.getId());
        response.put("subtotal", savedSale.getSubtotal());
        response.put("discount", savedSale.getDiscount());
        response.put("total", savedSale.getTotal());

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public List<Sale> getAll(HttpServletRequest req) {
        Long companyId = (Long) req.getAttribute("companyId");
        List<Sale> sales = saleRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);
        System.out.println("Ventas: " + sales.size());
        return sales;
    }

    @GetMapping("/{saleId}")
    public Sale getById(@PathVariable Long saleId, HttpServletRequest request){
        Long companyId = (Long) request.getAttribute("companyId");
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow();
        if (!sale.getCompany().getId().equals(companyId)){
            throw new RuntimeException("Venta no autorizada");
        }
        return sale;
    }
    

    @GetMapping("/payment-stats")
    public Map<String, Double> paymentStats(HttpServletRequest request) {

        Long companyId = (Long) request.getAttribute("companyId");

        List<Sale> sales =
            saleRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);

        Map<String, Double> stats = new HashMap<>();

        stats.put("cash", 0.0);
        stats.put("transfer", 0.0);
        stats.put("debit", 0.0);
        stats.put("credit", 0.0);

        for (Sale sale : sales) {
            if (sale.getPayments() == null) continue;

            for (SalePayment payment : sale.getPayments()) {
                
                switch (payment.getMethod()) {
                    case CASH -> stats.put("cash",stats.get("cash") + payment.getAmount());
                    case TRANSFER -> stats.put("transfer", stats.get("transfer") + payment.getAmount());
                    case DEBIT -> stats.put("debit", stats.get("debit") + payment.getAmount());
                    case CREDIT -> stats.put("credit", stats.get("credit") + payment.getAmount());
                        
                }
            }
        }

        return stats;
    }

    @GetMapping("/stats")
    public SalesStatsDTO stats(HttpServletRequest request) {

        Long companyId =
            (Long) request.getAttribute("companyId");

        List<Sale> sales =
            saleRepository.findByCompany_IdOrderByCreatedAtDesc(companyId);

        LocalDate today = LocalDate.now();

        SalesStatsDTO dto = new SalesStatsDTO();

        dto.today = sales.stream()
            .filter(s ->
                s.getCreatedAt() != null &&
                s.getCreatedAt()
                .toLocalDate()
                .equals(today)
            )
            .mapToDouble(Sale::getTotal)
            .sum();

        dto.week = sales.stream()
            .filter(s ->
                s.getCreatedAt() != null &&
                s.getCreatedAt()
                .isAfter(LocalDateTime.now().minusDays(7))
            )
            .mapToDouble(Sale::getTotal)
            .sum();

        dto.month = sales.stream()
            .filter(s ->
                s.getCreatedAt() != null &&
                s.getCreatedAt()
                .isAfter(LocalDateTime.now().minusDays(30))
            )
            .mapToDouble(Sale::getTotal)
            .sum();

        dto.total = sales.stream()
            .mapToDouble(Sale::getTotal)
            .sum();

        return dto;
    }

    
    @GetMapping("/report")
    public SalesReportDTO report(
            @RequestParam String from,
            @RequestParam String to,
            HttpServletRequest request) {

        Long companyId =
            (Long) request.getAttribute("companyId");

        LocalDateTime fromDate =
            LocalDateTime.parse(from);

        LocalDateTime toDate =
            LocalDateTime.parse(to);

        List<Sale> sales =
            saleRepository.findByCompany_IdOrderByCreatedAtDesc(companyId)
                .stream()
                .filter(s ->
                    s.getCreatedAt() != null &&
                    s.getCreatedAt().isAfter(fromDate) &&
                    s.getCreatedAt().isBefore(toDate)
                )
                .toList();

        SalesReportDTO dto = new SalesReportDTO();

        dto.total = sales.stream()
            .mapToDouble(Sale::getTotal)
            .sum();

        dto.salesCount = (long) sales.size();

        dto.averageTicket =
            dto.salesCount > 0
                ? dto.total / dto.salesCount
                : 0;

        dto.productsSold = sales.stream()
            .flatMap(s -> s.getItems().stream())
            .mapToInt(SaleItem::getQuantity)
            .sum();
        
        for (Sale sale : sales) {

            if (sale.getPayments() == null) continue;

            for (SalePayment payment : sale.getPayments()) {

                switch (payment.getMethod()) {

                    case CASH ->
                        dto.cash += payment.getAmount();

                    case TRANSFER ->
                        dto.transfer += payment.getAmount();

                    case DEBIT ->
                        dto.debit += payment.getAmount();

                    case CREDIT ->
                        dto.credit += payment.getAmount();
                }
            }
        }

        dto.sales = sales;
        return dto;
    }
}