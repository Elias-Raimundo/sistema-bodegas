package bodega_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.SupplierInvoice;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {

    List<SupplierInvoice> findBySupplier_IdOrderByInvoiceDateDesc(Long supplierId);

    List<SupplierInvoice> findBySupplier_Company_IdOrderByInvoiceDateDesc(Long companyId);
}