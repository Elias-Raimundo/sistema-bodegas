package bodega_system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import bodega_system.entity.Company;
import bodega_system.entity.Supplier;
import bodega_system.entity.SupplierInvoice;
import bodega_system.repository.SupplierInvoiceRepository;
import bodega_system.repository.SupplierRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final SupplierInvoiceRepository invoiceRepository;

    public SupplierController(
        SupplierRepository supplierRepository,
        SupplierInvoiceRepository invoiceRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public List<Supplier> getAll(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        return supplierRepository.findByCompany_IdOrderByNameAsc(companyId);
    }

    @PostMapping
    public Supplier createSupplier(
        @RequestBody Supplier supplier,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

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
        @RequestBody SupplierInvoice invoice,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

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

    @DeleteMapping("/invoices/{invoiceId}")
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

        invoiceRepository.delete(invoice);
    }
}
