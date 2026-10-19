package bodega_system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import bodega_system.dto.PurchaseDTO;
import bodega_system.entity.Category;
import bodega_system.entity.Company;
import bodega_system.entity.Product;
import bodega_system.entity.Supplier;
import bodega_system.entity.SupplierInvoice;
import bodega_system.entity.SupplierInvoiceItem;
import bodega_system.repository.CategoryRepository;
import bodega_system.repository.CompanyRepository;
import bodega_system.repository.ProductRepository;
import bodega_system.repository.SupplierInvoiceRepository;
import bodega_system.repository.SupplierRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final SupplierInvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;

    public SupplierController(
        SupplierRepository supplierRepository,
        SupplierInvoiceRepository invoiceRepository,
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        CompanyRepository companyRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public List<Supplier> getAll(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        List<Supplier> suppliers = supplierRepository.findByCompany_IdOrderByNameAsc(companyId);

        if (suppliers.isEmpty()) {
            return suppliers;
        }

        List<SupplierInvoice> allInvoices =
            invoiceRepository.findBySupplier_Company_IdOrderByInvoiceDateDesc(companyId);

        Map<Long, Double> pendingBySupplier = allInvoices.stream()
            .collect(Collectors.groupingBy(
                invoice -> invoice.getSupplier().getId(),
                Collectors.summingDouble(SupplierInvoice::getPendingAmount)
            ));

        for (Supplier supplier : suppliers) {
            supplier.setPendingTotal(pendingBySupplier.getOrDefault(supplier.getId(), 0.0));
        }

        return suppliers;
    }

    @PostMapping
    public Supplier createSupplier(
        @RequestBody Supplier input,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        // Proveedor NUEVO con solo los campos permitidos (sin "id" del cliente)
        Supplier supplier = new Supplier();
        supplier.setName(input.getName());
        supplier.setDescription(input.getDescription());

        if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del proveedor es obligatorio");
        }

        Company company = new Company();
        company.setId(companyId);

        supplier.setName(supplier.getName().trim());
        supplier.setCompany(company);

        return supplierRepository.save(supplier);
    }

    @GetMapping("/invoices")
    public List<SupplierInvoice> getAllInvoices(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        return invoiceRepository.findBySupplier_Company_IdOrderByInvoiceDateDesc(companyId);
    }

    @PutMapping("/{supplierId}")
    public Supplier updateSupplier(
        @PathVariable Long supplierId,
        @RequestBody Supplier data,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow();

        if (!supplier.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Proveedor no autorizado");
        }

        if (data.getName() == null || data.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del proveedor es obligatorio");
        }

        supplier.setName(data.getName().trim());
        supplier.setDescription(data.getDescription());

        return supplierRepository.save(supplier);
    }

    @DeleteMapping("/{supplierId}")
    public void deleteSupplier(
        @PathVariable Long supplierId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow();

        if (!supplier.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Proveedor no autorizado");
        }

        List<SupplierInvoice> invoices =
            invoiceRepository.findBySupplier_IdOrderByInvoiceDateDesc(supplierId);

        if (!invoices.isEmpty()) {
            throw new RuntimeException("No se puede eliminar un proveedor con facturas cargadas");
        }

        supplierRepository.delete(supplier);
    }

    @GetMapping("/{supplierId}/invoices")
    public List<SupplierInvoice> getInvoicesBySupplier(
        @PathVariable Long supplierId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow();

        if (!supplier.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Proveedor no autorizado");
        }

        return invoiceRepository.findBySupplier_IdOrderByInvoiceDateDesc(supplierId);
    }

    @PostMapping("/{supplierId}/invoices")
    public SupplierInvoice createInvoice(
        @PathVariable Long supplierId,
        @RequestBody SupplierInvoice input,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        // Factura NUEVA con solo los campos permitidos (sin "id" del cliente)
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setInvoiceNumber(input.getInvoiceNumber());
        invoice.setInvoiceDate(input.getInvoiceDate());
        invoice.setTotalAmount(input.getTotalAmount());
        invoice.setPaidAmount(input.getPaidAmount());
        invoice.setDescription(input.getDescription());

        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow();

        if (!supplier.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Proveedor no autorizado");
        }

        if (invoice.getTotalAmount() == null || invoice.getTotalAmount() <= 0) {
            throw new RuntimeException("El monto total debe ser mayor a cero");
        }

        if (invoice.getPaidAmount() == null) {
            invoice.setPaidAmount(0.0);
        }

        if (invoice.getPaidAmount() < 0) {
            throw new RuntimeException("El monto pagado no puede ser negativo");
        }

        if (invoice.getPaidAmount() > invoice.getTotalAmount()) {
            throw new RuntimeException("El monto pagado no puede ser mayor al total");
        }

        invoice.setSupplier(supplier);

        return invoiceRepository.save(invoice);
    }

    @PostMapping("/invoices/{invoiceId}/payment")
    public SupplierInvoice payInvoice(
        @PathVariable Long invoiceId,
        @RequestBody SupplierInvoice payment,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        SupplierInvoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow();

        if (!invoice.getSupplier().getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Factura no autorizada");
        }

        if (payment.getPaidAmount() == null || payment.getPaidAmount() <= 0) {
            throw new RuntimeException("El pago debe ser mayor a cero");
        }

        double currentPaid = invoice.getPaidAmount() == null ? 0 : invoice.getPaidAmount();
        double total = invoice.getTotalAmount() == null ? 0 : invoice.getTotalAmount();
        double newPaid = currentPaid + payment.getPaidAmount();

        if (newPaid > total) {
            throw new RuntimeException("El pago supera el saldo pendiente");
        }

        invoice.setPaidAmount(newPaid);

        return invoiceRepository.save(invoice);
    }

    // COMPRA A PROVEEDOR CON PRODUCTOS
    // Crea la factura con su detalle, suma el stock, actualiza el costo de
    // cada producto y, si se indica, el precio de venta. Los productos que
    // no existen se crean en el momento.
    @PostMapping("/{supplierId}/purchases")
    @Transactional
    public SupplierInvoice createPurchase(
        @PathVariable Long supplierId,
        @RequestBody PurchaseDTO dto,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        Supplier supplier = supplierRepository.findById(supplierId)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        if (!supplier.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Proveedor no autorizado");
        }

        if (dto.items == null || dto.items.isEmpty()) {
            throw new RuntimeException("Agregá al menos un producto a la compra");
        }

        Company company = companyRepository.findById(companyId).orElseThrow();

        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplier(supplier);

        double total = 0;
        int position = 0;

        for (PurchaseDTO.Item line : dto.items) {
            position++;

            if (line.quantity == null || line.quantity <= 0) {
                throw new RuntimeException("Renglón " + position + ": la cantidad debe ser mayor a cero");
            }
            if (line.unitCost == null || line.unitCost < 0) {
                throw new RuntimeException("Renglón " + position + ": el costo no puede ser negativo");
            }
            if (line.salePrice != null && line.salePrice < 0) {
                throw new RuntimeException("Renglón " + position + ": el precio de venta no puede ser negativo");
            }

            Product product;

            if (line.productId != null) {
                product = productRepository.findById(line.productId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                if (!product.getCompany().getId().equals(companyId)) {
                    throw new RuntimeException("Producto no autorizado");
                }
            } else {
                String name = line.newName == null ? "" : line.newName.trim();
                if (name.isEmpty()) {
                    throw new RuntimeException("Renglón " + position + ": falta el nombre del producto nuevo");
                }

                // Si ya existe uno con ese nombre, se usa ese en vez de duplicarlo
                product = productRepository.findByNameIgnoreCaseAndCompanyId(name, companyId)
                    .orElse(null);

                if (product == null) {
                    if (line.salePrice == null) {
                        throw new RuntimeException("Indicá el precio de venta de \"" + name + "\"");
                    }

                    product = new Product();
                    product.setName(name);
                    product.setStock(0.0);
                    product.setPrice(line.salePrice);
                    product.setCompany(company);

                    if (line.newCategoryId != null) {
                        Category category = categoryRepository
                            .findByIdAndCompany(line.newCategoryId, company)
                            .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
                        product.setCategory(category);
                    }
                }
            }

            double stock = product.getStock() == null ? 0 : product.getStock();
            product.setStock(stock + line.quantity);
            product.setCostPrice(line.unitCost);

            if (line.salePrice != null) {
                product.setPrice(line.salePrice);
            }

            product = productRepository.save(product);

            SupplierInvoiceItem item = new SupplierInvoiceItem();
            item.setInvoice(invoice);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(line.quantity);
            item.setUnitCost(line.unitCost);
            item.setSalePrice(line.salePrice);
            invoice.getItems().add(item);

            total += line.quantity * line.unitCost;
        }

        total = Math.round(total * 100) / 100.0;

        double paid = dto.paidAmount == null ? 0 : dto.paidAmount;
        if (paid < 0) {
            throw new RuntimeException("El pago no puede ser negativo");
        }
        if (paid > total + 0.01) {
            throw new RuntimeException("El pago no puede ser mayor al total de la compra");
        }

        java.time.LocalDate date = java.time.LocalDate.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires"));
        if (dto.invoiceDate != null && !dto.invoiceDate.isBlank()) {
            try {
                date = java.time.LocalDate.parse(dto.invoiceDate.trim());
            } catch (Exception e) {
                throw new RuntimeException("Fecha de factura inválida");
            }
        }

        invoice.setInvoiceNumber(dto.invoiceNumber == null ? null : dto.invoiceNumber.trim());
        invoice.setInvoiceDate(date);
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(Math.min(paid, total));
        invoice.setDescription(
            dto.description != null && !dto.description.isBlank()
                ? dto.description.trim()
                : "Compra de " + dto.items.size() + (dto.items.size() == 1 ? " producto" : " productos")
        );

        return invoiceRepository.save(invoice);
    }

    @DeleteMapping("/invoices/{invoiceId}")
    @Transactional
    public void deleteInvoice(
        @PathVariable Long invoiceId,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        SupplierInvoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow();

        if (!invoice.getSupplier().getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Factura no autorizada");
        }

        // Si era una compra con productos, se descuenta el stock que había
        // sumado (los costos y precios no se tocan)
        for (SupplierInvoiceItem item : invoice.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null) continue;

            productRepository.findById(item.getProductId()).ifPresent(product -> {
                if (!product.getCompany().getId().equals(companyId)) return;
                double stock = product.getStock() == null ? 0 : product.getStock();
                product.setStock(stock - item.getQuantity());
                productRepository.save(product);
            });
        }

        invoiceRepository.delete(invoice);
    }
}
