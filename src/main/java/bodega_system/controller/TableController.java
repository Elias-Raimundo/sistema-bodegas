package bodega_system.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import bodega_system.dto.CloseTableDTO;
import bodega_system.dto.TableItemDTO;
import bodega_system.entity.*;
import bodega_system.repository.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/tables")
public class TableController {

    private final TableBarRepository tableBarRepository;
    private final TableOrderRepository tableOrderRepository;
    private final TableOrderItemRepository tableOrderItemRepository;
    private final ProductRepository productRepository;
    private final PreparedProductRepository preparedProductRepository;
    private final SaleRepository saleRepository;

    public TableController(
        TableBarRepository tableBarRepository,
        TableOrderRepository tableOrderRepository,
        TableOrderItemRepository tableOrderItemRepository,
        ProductRepository productRepository,
        PreparedProductRepository preparedProductRepository,
        SaleRepository saleRepository
    ) {
        this.tableBarRepository = tableBarRepository;
        this.tableOrderRepository = tableOrderRepository;
        this.tableOrderItemRepository = tableOrderItemRepository;
        this.productRepository = productRepository;
        this.preparedProductRepository = preparedProductRepository;
        this.saleRepository = saleRepository;
    }

    @PostMapping("/init")
    public List<TableBar> initTables(
        @RequestParam int count,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        if (count <= 0) {
            throw new RuntimeException("La cantidad de mesas debe ser mayor a cero");
        }

        if (count > 100) {
            throw new RuntimeException("No podés crear más de 100 mesas de una vez");
        }

        Company company = new Company();
        company.setId(companyId);

        List<TableBar> existingTables =
            tableBarRepository.findByCompany(company);

        int startNumber = existingTables.size() + 1;

        List<TableBar> tables = new ArrayList<>();

        for (int i = startNumber; i < startNumber + count; i++) {
            TableBar table = new TableBar();

            table.setName("Mesa " + i);
            table.setOccupied(false);
            table.setCompany(company);

            tables.add(tableBarRepository.save(table));
        }

        return tables;
    }

    @GetMapping
    public List<TableBar> getTables(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        Company company = new Company();
        company.setId(companyId);

        return tableBarRepository.findByCompany(company);
    }

    @PutMapping("/{tableId}")
    public TableBar updateTable(
        @PathVariable Long tableId,
        @RequestBody TableBar data,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        TableBar table = tableBarRepository.findById(tableId)
            .orElseThrow();

        if (!table.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        if (data.getName() == null || data.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre de la mesa es obligatorio");
        }

        table.setName(data.getName().trim());

        return tableBarRepository.save(table);
    }

    @DeleteMapping("/{tableId}")
    public ResponseEntity<Void> deleteTable(
        @PathVariable Long tableId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        TableBar table = tableBarRepository.findById(tableId)
            .orElseThrow();

        if (!table.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        if (table.isOccupied()) {
            throw new RuntimeException("No se puede eliminar una mesa ocupada");
        }

        List<TableOrder> orders =
            tableOrderRepository.findByTable(table);

        tableOrderRepository.deleteAll(orders);
        tableBarRepository.delete(table);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{tableId}/order")
    public TableOrder getOpenOrder(
        @PathVariable Long tableId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        TableBar table = tableBarRepository.findById(tableId)
            .orElseThrow();

        if (!table.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        return tableOrderRepository
            .findByTableAndClosedFalse(table)
            .orElseGet(() -> {
                TableOrder order = new TableOrder();

                order.setTable(table);
                order.setClosed(false);
                order.setItems(new ArrayList<>());

                return tableOrderRepository.save(order);
            });
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
        @PathVariable Long itemId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        TableOrderItem item = tableOrderItemRepository
            .findById(itemId)
            .orElseThrow();

        TableOrder order = item.getOrder();
        TableBar table = order.getTable();

        if (!table.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        order.getItems().removeIf(i ->
            i.getId().equals(itemId)
        );

        tableOrderItemRepository.delete(item);
        tableOrderItemRepository.flush();

        TableOrder updatedOrder =
            tableOrderRepository.findById(order.getId())
                .orElseThrow();

        if (
            updatedOrder.getItems() == null ||
            updatedOrder.getItems().isEmpty()
        ) {
            table.setOccupied(false);
        } else {
            table.setOccupied(true);
        }

        tableBarRepository.save(table);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tableId}/items")
    public TableOrder addItem(
        @PathVariable Long tableId,
        @RequestBody TableItemDTO dto,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        TableBar table = tableBarRepository.findById(tableId)
            .orElseThrow();

        if (!table.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        if (dto.quantity <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a cero");
        }

        String itemType =
            dto.itemType == null ? "PRODUCT" : dto.itemType;

        if (!itemType.equals("PRODUCT") && !itemType.equals("PREPARED")) {
            throw new RuntimeException("Tipo de producto inválido");
        }

        TableOrder order = tableOrderRepository
            .findByTableAndClosedFalse(table)
            .orElseGet(() -> {
                TableOrder newOrder = new TableOrder();

                newOrder.setTable(table);
                newOrder.setClosed(false);
                newOrder.setItems(new ArrayList<>());

                return tableOrderRepository.save(newOrder);
            });

        TableOrderItem existing = order.getItems()
            .stream()
            .filter(i ->
                itemType.equals(i.getItemType()) &&
                (
                    itemType.equals("PRODUCT")
                        ? i.getProductId() != null &&
                        i.getProductId().equals(dto.productId)
                        : i.getPreparedProductId() != null &&
                        i.getPreparedProductId().equals(dto.preparedProductId)
                )
            )
            .findFirst()
            .orElse(null);

        if (itemType.equals("PRODUCT")) {

            if (dto.productId == null) {
                throw new RuntimeException("Debe seleccionar un producto");
            }

            Product product = productRepository
                .findById(dto.productId)
                .orElseThrow();

            if (!product.getCompany().getId().equals(companyId)) {
                throw new RuntimeException("Producto no autorizado");
            }

            int finalQuantity =
                existing != null
                    ? existing.getQuantity() + dto.quantity
                    : dto.quantity;

            if (product.getStock() < finalQuantity) {
                throw new RuntimeException(
                    "Stock insuficiente para " + product.getName()
                );
            }

            if (existing != null) {
                existing.setQuantity(finalQuantity);
                tableOrderItemRepository.save(existing);
            } else {
                TableOrderItem item = new TableOrderItem();

                item.setQuantity(dto.quantity);
                item.setOrder(order);
                item.setItemType("PRODUCT");
                item.setProductId(product.getId());
                item.setPreparedProductId(null);
                item.setProductName(product.getName());
                item.setPrice(product.getPrice());

                tableOrderItemRepository.save(item);
            }
        }

        if (itemType.equals("PREPARED")) {

            if (dto.preparedProductId == null) {
                throw new RuntimeException("Debe seleccionar un preparado");
            }

            PreparedProduct prepared =
                preparedProductRepository
                    .findById(dto.preparedProductId)
                    .orElseThrow();

            if (!prepared.getCompany().getId().equals(companyId)) {
                throw new RuntimeException("Preparado no autorizado");
            }

            if (prepared.getIngredients() == null || prepared.getIngredients().isEmpty()) {
                throw new RuntimeException("El preparado no tiene ingredientes");
            }

            int finalQuantity =
                existing != null
                    ? existing.getQuantity() + dto.quantity
                    : dto.quantity;

            for (PreparedProductIngredient ingredient : prepared.getIngredients()) {

                Product product = ingredient.getProduct();

                if (!product.getCompany().getId().equals(companyId)) {
                    throw new RuntimeException("Ingrediente no autorizado");
                }

                double stockToDiscount =
                    ingredient.getQuantity() * finalQuantity;

                if (product.getStock() < stockToDiscount) {
                    throw new RuntimeException(
                        "Stock insuficiente para " + product.getName()
                    );
                }
            }

            if (existing != null) {
                existing.setQuantity(finalQuantity);
                tableOrderItemRepository.save(existing);
            } else {
                TableOrderItem item = new TableOrderItem();

                item.setQuantity(dto.quantity);
                item.setOrder(order);
                item.setItemType("PREPARED");
                item.setProductId(null);
                item.setPreparedProductId(prepared.getId());
                item.setProductName(prepared.getName());
                item.setPrice(prepared.getPrice());

                tableOrderItemRepository.save(item);
            }
        }

        table.setOccupied(true);
        tableBarRepository.save(table);

        return tableOrderRepository.findById(order.getId())
            .orElseThrow();
    }

    @PutMapping("/items/{itemId}/quantity")
    public TableOrder updateItemQuantity(
        @PathVariable Long itemId,
        @RequestParam int quantity,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        TableOrderItem item = tableOrderItemRepository
            .findById(itemId)
            .orElseThrow();

        TableOrder order = item.getOrder();
        TableBar table = order.getTable();

        if (!table.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        if (quantity <= 0) {

            order.getItems().removeIf(i ->
                i.getId().equals(itemId)
            );

            tableOrderItemRepository.delete(item);
            tableOrderItemRepository.flush();

        } else {

            if (item.getItemType().equals("PRODUCT")) {

                Product product = productRepository
                    .findById(item.getProductId())
                    .orElseThrow();

                if (!product.getCompany().getId().equals(companyId)) {
                    throw new RuntimeException("Producto no autorizado");
                }

                if (product.getStock() < quantity) {
                    throw new RuntimeException(
                        "Stock insuficiente para " + product.getName()
                    );
                }
            }

            if (item.getItemType().equals("PREPARED")) {

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
                        ingredient.getQuantity() * quantity;

                    if (product.getStock() < stockToDiscount) {
                        throw new RuntimeException(
                            "Stock insuficiente para " + product.getName()
                        );
                    }
                }
            }

            item.setQuantity(quantity);
            tableOrderItemRepository.save(item);
        }

        TableOrder updatedOrder = tableOrderRepository
            .findById(order.getId())
            .orElseThrow();

        if (
            updatedOrder.getItems() == null ||
            updatedOrder.getItems().isEmpty()
        ) {
            table.setOccupied(false);
        } else {
            table.setOccupied(true);
        }

        tableBarRepository.save(table);

        return updatedOrder;
    }

    @PostMapping("/{tableId}/close")
    public ResponseEntity<Void> closeTable(
        @PathVariable Long tableId,
        @RequestBody CloseTableDTO dto,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Company company = new Company();
        company.setId(companyId);

        TableBar table = tableBarRepository.findById(tableId)
            .orElseThrow();

        if (!table.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }

        TableOrder order = tableOrderRepository
            .findByTableAndClosedFalse(table)
            .orElseThrow();

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new RuntimeException("La mesa no tiene productos");
        }

        if (dto.payments == null || dto.payments.isEmpty()) {
            throw new RuntimeException("Debe registrar al menos un pago");
        }

        for (SalePayment payment : dto.payments) {
            if (payment.getAmount() <= 0) {
                throw new RuntimeException("El importe del pago debe ser mayor a cero");
            }
        }

        Sale sale = new Sale();
        sale.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires")));
        sale.setCompany(company);

        List<SaleItem> saleItems = new ArrayList<>();
        double subtotal = 0;

        for (TableOrderItem tableItem : order.getItems()) {

            SaleItem saleItem = new SaleItem();

            saleItem.setItemType(tableItem.getItemType());
            saleItem.setProductId(tableItem.getProductId());
            saleItem.setPreparedProductId(tableItem.getPreparedProductId());
            saleItem.setProductName(tableItem.getProductName());
            saleItem.setQuantity(tableItem.getQuantity());
            saleItem.setPrice(tableItem.getPrice());
            saleItem.setSale(sale);

            if (tableItem.getItemType().equals("PRODUCT")) {

                Product product = productRepository
                    .findById(tableItem.getProductId())
                    .orElseThrow();

                if (!product.getCompany().getId().equals(companyId)) {
                    throw new RuntimeException("Producto no autorizado");
                }

                if (product.getStock() < tableItem.getQuantity()) {
                    throw new RuntimeException(
                        "Stock insuficiente para " + product.getName()
                    );
                }

            } else if (tableItem.getItemType().equals("PREPARED")) {

                PreparedProduct prepared =
                    preparedProductRepository
                        .findById(tableItem.getPreparedProductId())
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
                        ingredient.getQuantity() * tableItem.getQuantity();

                    if (product.getStock() < stockToDiscount) {
                        throw new RuntimeException(
                            "Stock insuficiente para " + product.getName()
                        );
                    }
                }
            }

            subtotal += tableItem.getQuantity() * tableItem.getPrice();
            saleItems.add(saleItem);
        }
        double discount = 
            dto.discount == null
                ? 0
                : dto.discount;
        
        if (discount < 0){
            throw new RuntimeException("El descuento no puede ser negativo");
        }
        if (discount > subtotal){
            throw new RuntimeException("El descuento no puede ser mayor al subtotal");
        }

        double total = subtotal - discount;

        double paymentsTotal = dto.payments
            .stream()
            .mapToDouble(SalePayment::getAmount)
            .sum();

        if (Math.abs(paymentsTotal - total) > 0.01) {
            throw new RuntimeException("Los pagos no coinciden con el total");
        }

        for (TableOrderItem tableItem : order.getItems()) {

            if (tableItem.getItemType().equals("PRODUCT")) {

                Product product = productRepository
                    .findById(tableItem.getProductId())
                    .orElseThrow();

                product.setStock(
                    product.getStock() - tableItem.getQuantity()
                );

                productRepository.save(product);

            } else if (tableItem.getItemType().equals("PREPARED")) {

                PreparedProduct prepared =
                    preparedProductRepository
                        .findById(tableItem.getPreparedProductId())
                        .orElseThrow();

                for (PreparedProductIngredient ingredient : prepared.getIngredients()) {

                    Product product = ingredient.getProduct();

                    double stockToDiscount =
                        ingredient.getQuantity() * tableItem.getQuantity();

                    product.setStock(product.getStock() - stockToDiscount);
                    productRepository.save(product);
                }
            }
        }

        sale.setItems(saleItems);
        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(total);

        for (SalePayment payment : dto.payments) {
            payment.setSale(sale);
        }

        sale.setPayments(dto.payments);

        saleRepository.save(sale);

        order.setClosed(true);
        tableOrderRepository.save(order);

        table.setOccupied(false);
        tableBarRepository.save(table);

        return ResponseEntity.ok().build();
    }
}