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
import bodega_system.enums.PaymentMethod;
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
    public ResponseEntity<?> create(@RequestBody Sale input, HttpServletRequest req) {

        // Una venta hecha sin conexión ya se cobró: al sincronizarla no se
        // rechaza por falta de stock (puede quedar en negativo, igual que en mesas).
        boolean offline = bodega_system.config.OfflineTime.isOffline(req);

        // Armamos una venta NUEVA copiando solo lo que el cliente puede elegir:
        // qué productos, cuántos, descuento, cliente y formas de pago.
        // Precios, costos, totales y fecha los calcula siempre el servidor.
        Sale sale = new Sale();
        sale.setDiscount(input.getDiscount());
        sale.setCustomerId(input.getCustomerId());

        if (input.getItems() != null) {
            List<SaleItem> items = new java.util.ArrayList<>();
            for (SaleItem in : input.getItems()) {
                SaleItem item = new SaleItem();
                item.setItemType(in.getItemType());
                item.setProductId(in.getProductId());
                item.setPreparedProductId(in.getPreparedProductId());
                item.setQuantity(in.getQuantity());
                items.add(item);
            }
            sale.setItems(items);
        }

        if (input.getPayments() != null) {
            List<SalePayment> payments = new java.util.ArrayList<>();
            for (SalePayment in : input.getPayments()) {
                SalePayment payment = new SalePayment();
                payment.setMethod(in.getMethod());
                payment.setAmount(in.getAmount());
                payments.add(payment);
            }
            sale.setPayments(payments);
        }

        Long companyId = (Long) req.getAttribute("companyId");

        Company company = new Company();
        company.setId(companyId);

        double subtotal = 0;

        sale.setCreatedAt(bodega_system.config.OfflineTime.resolve(req));
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
                if (!offline && product.getStock() < item.getQuantity()){
                    throw new RuntimeException(
                        "Stock insuficiente para " + product.getName()
                    );
                }

                product.setStock(product.getStock() - item.getQuantity());
                productRepository.save(product);

                item.setProductName(product.getName());
                item.setPrice(product.getPrice());
                item.setCostPrice(product.getCostPrice() != null ? product.getCostPrice() : 0.0);
                item.setItemType("PRODUCT");
                item.setStockUnits(item.getQuantity());

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

                    if (!offline && product.getStock() < stockToDiscount) {
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
                item.setCostPrice(prepared.getCostPrice() != null ? prepared.getCostPrice() : 0.0);
                item.setItemType("PREPARED");
                item.setStockUnits(item.getQuantity());

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
            movement.setCreatedAt(bodega_system.config.OfflineTime.resolve(req));

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
    public List<Sale> getAll(
        @RequestParam(required = false) Integer limit,   
        HttpServletRequest req
    ) {
        Long companyId = (Long) req.getAttribute("companyId");

        if (limit != null) {
            var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
            return saleRepository.findActiveByCompany(companyId, pageable);
        }

        List<Sale> sales = saleRepository.findActiveByCompany(companyId);
        return sales;
    }

    // ANULAR VENTA
    // La venta no se borra: queda marcada como anulada (con motivo y hora),
    // se devuelve el stock y se saca la deuda de cuenta corriente que generó.
    // Deja de contar en totales, caja, reportes y dashboard.
    @PostMapping("/{saleId}/cancel")
    @Transactional
    public Map<String, Object> cancel(
        @PathVariable Long saleId,
        @RequestBody(required = false) Map<String, String> body,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (!sale.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Venta no autorizada");
        }

        if (sale.isCancelled()) {
            throw new RuntimeException("La venta ya está anulada");
        }

        String reason = body == null ? null : body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Indicá el motivo de la anulación");
        }
        reason = reason.trim();
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }

        // 1) Devolver el stock
        for (SaleItem item : sale.getItems()) {
            int units;
            if (item.getStockUnits() != null) {
                units = item.getStockUnits();
            } else {
                // Ventas anteriores a este cambio: los ítems compartidos entre
                // mesas no se sabe cuánto descontaron, así que no se tocan.
                String name = item.getProductName() == null ? "" : item.getProductName();
                units = name.contains("(compartido") ? 0 : item.getQuantity();
            }

            if (units <= 0) continue;

            if ("PREPARED".equals(item.getItemType())) {
                if (item.getPreparedProductId() == null) continue;
                preparedProductRepository.findById(item.getPreparedProductId()).ifPresent(prepared -> {
                    if (!prepared.getCompany().getId().equals(companyId)) return;
                    for (PreparedProductIngredient ingredient : prepared.getIngredients()) {
                        Product product = ingredient.getProduct();
                        product.setStock(product.getStock() + ingredient.getQuantity() * units);
                        productRepository.save(product);
                    }
                });
            } else {
                if (item.getProductId() == null) continue;
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    if (!product.getCompany().getId().equals(companyId)) return;
                    product.setStock(product.getStock() + units);
                    productRepository.save(product);
                });
            }
        }

        // 2) Sacar la deuda de cuenta corriente que generó esta venta
        double removedDebt = 0;
        for (CustomerMovement movement : customerMovementRepository.findBySaleId(sale.getId())) {
            if (!"DEBT".equals(movement.getType())) continue;

            Customer customer = movement.getCustomer();
            if (customer != null && customer.getCompany().getId().equals(companyId)) {
                double balance = customer.getBalance() == null ? 0 : customer.getBalance();
                customer.setBalance(Math.max(balance - movement.getAmount(), 0));
                customerRepository.save(customer);
            }

            removedDebt += movement.getAmount();
            customerMovementRepository.delete(movement);
        }

        // 3) Marcar la venta como anulada
        sale.setCancelled(true);
        sale.setCancelledAt(LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")));
        sale.setCancelReason(reason);
        saleRepository.save(sale);

        Map<String, Object> response = new HashMap<>();
        response.put("id", sale.getId());
        response.put("cancelled", true);

        // Pagos parciales de mesa a cuenta corriente: se cargan como deuda
        // suelta (sin número de venta) y no se pueden revertir solos.
        double currentAccountPaid = sale.getPayments() == null ? 0 : sale.getPayments().stream()
            .filter(p -> p.getMethod() != null && p.getMethod().name().equals("CURRENT_ACCOUNT"))
            .mapToDouble(SalePayment::getAmount)
            .sum();

        if (currentAccountPaid - removedDebt > 0.01) {
            response.put("warning",
                "Se anuló la venta, pero $" + Math.round(currentAccountPaid - removedDebt) +
                " se habían cargado a cuenta corriente como pago parcial de mesa. " +
                "Revisá la cuenta del cliente y ajustalo con un pago."
            );
        }

        return response;
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

        Map<String, Double> stats = new HashMap<>();
        stats.put("cash", 0.0);
        stats.put("transfer", 0.0);
        stats.put("debit", 0.0);
        stats.put("credit", 0.0);

        for (Object[] row : saleRepository.sumPaymentsByMethod(companyId)) {
            PaymentMethod method = (PaymentMethod) row[0];
            double amount = ((Number) row[1]).doubleValue();
            switch (method) {
                case CASH -> stats.put("cash", amount);
                case TRANSFER -> stats.put("transfer", amount);
                case DEBIT -> stats.put("debit", amount);
                case CREDIT -> stats.put("credit", amount);
                default -> {}
            }
        }

        return stats;
    }

    @GetMapping("/stats")
    public SalesStatsDTO stats(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        SalesStatsDTO dto = new SalesStatsDTO();
        dto.today = saleRepository.sumTotalSince(companyId, startOfToday);
        dto.week  = saleRepository.sumTotalSince(companyId, sevenDaysAgo);
        dto.month = saleRepository.sumTotalSince(companyId, thirtyDaysAgo);
        dto.total = saleRepository.sumTotalAll(companyId);

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

        List<Sale> allSales =
            saleRepository.findByCompany_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
                companyId, fromDate, toDate
            );

        // Las anuladas van en una lista aparte y no cuentan en los totales
        List<Sale> sales = allSales.stream().filter(s -> !s.isCancelled()).toList();
        List<Sale> cancelledSales = allSales.stream().filter(Sale::isCancelled).toList();

        List<Long> customerIds = allSales.stream()
            .map(Sale::getCustomerId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, String> customerNames = customerRepository.findAllById(customerIds)
            .stream()
            .collect(java.util.stream.Collectors.toMap(Customer::getId, Customer::getName));

        for (Sale sale : allSales) {
            if (sale.getCustomerId() != null) {
                sale.setCustomerName(customerNames.get(sale.getCustomerId()));
            }
        }
        SalesReportDTO dto = new SalesReportDTO();

        dto.total = sales.stream()
            .mapToDouble(Sale::getTotal)
            .sum();

        dto.salesCount = (long) sales.size();

        dto.totalProfit = sales.stream()
            .mapToDouble(sale -> {
                double saleCost = sale.getItems().stream()
                    .mapToDouble(item -> (item.getCostPrice() != null ? item.getCostPrice() : 0.0) * item.getQuantity())
                    .sum();
                return sale.getTotal() - saleCost;
            })
            .sum();

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
        dto.cancelledSales = cancelledSales;
        return dto;
    }
}