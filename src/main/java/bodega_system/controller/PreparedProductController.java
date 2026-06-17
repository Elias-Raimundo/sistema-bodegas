package bodega_system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import bodega_system.dto.PreparedProductDTO;
import bodega_system.entity.Company;
import bodega_system.entity.PreparedProduct;
import bodega_system.entity.Product;
import bodega_system.repository.CompanyRepository;
import bodega_system.repository.PreparedProductRepository;
import bodega_system.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/prepared-products")
public class PreparedProductController {

    @Autowired
    private PreparedProductRepository preparedProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @GetMapping
    public List<PreparedProduct> getAll(HttpServletRequest request) {

        Long companyId =
            (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        return preparedProductRepository.findByCompany(company);
    }

    @PostMapping
    public PreparedProduct create(
            @RequestBody PreparedProductDTO dto,
            HttpServletRequest request) {

        Long companyId =
            (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        
        if (dto.name == null || dto.name.trim().isEmpty()) {
            throw new RuntimeException("El nombre es obligatorio");
        }

        if (dto.price == null || dto.price < 0) {
            throw new RuntimeException("El precio no puede ser negativo");
        }

        if (dto.servingsPerUnit == null || dto.servingsPerUnit <= 0) {
            throw new RuntimeException("Los vasos por botella deben ser mayor a cero");
        }

        if (dto.baseProductId == null) {
            throw new RuntimeException("Debe seleccionar una botella base");
        }

        Product baseProduct =
            productRepository.findById(dto.baseProductId)
                .orElseThrow();

        if (!baseProduct.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("Producto base no autorizado");
        }

        PreparedProduct preparedProduct =
            new PreparedProduct();

        preparedProduct.setName(dto.name.trim());
        preparedProduct.setPrice(dto.price);
        preparedProduct.setServingsPerUnit(dto.servingsPerUnit);
        preparedProduct.setBaseProduct(baseProduct);
        preparedProduct.setCompany(company);

        return preparedProductRepository.save(preparedProduct);
    }

    @PutMapping("/{id}")
    public PreparedProduct update(
            @PathVariable Long id,
            @RequestBody PreparedProductDTO dto,
            HttpServletRequest request) {

        Long companyId =
            (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        PreparedProduct preparedProduct =
            preparedProductRepository.findById(id)
                .orElseThrow();

        if (!preparedProduct.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("No autorizado");
        }

        if (dto.name == null || dto.name.trim().isEmpty()) {
            throw new RuntimeException("El nombre es obligatorio");
        }

        if (dto.price == null || dto.price < 0) {
            throw new RuntimeException("El precio no puede ser negativo");
        }

        if (dto.servingsPerUnit == null || dto.servingsPerUnit <= 0) {
            throw new RuntimeException("Los vasos por botella deben ser mayor a cero");
        }

        if (dto.baseProductId == null) {
            throw new RuntimeException("Debe seleccionar una botella base");
        }

        Product baseProduct =
            productRepository.findById(dto.baseProductId)
                .orElseThrow();

        if (!baseProduct.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("Producto base no autorizado");
        }

        preparedProduct.setName(dto.name.trim());
        preparedProduct.setPrice(dto.price);
        preparedProduct.setServingsPerUnit(dto.servingsPerUnit);
        preparedProduct.setBaseProduct(baseProduct);

        return preparedProductRepository.save(preparedProduct);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long companyId =
            (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        PreparedProduct preparedProduct =
            preparedProductRepository.findById(id)
                .orElseThrow();

        if (!preparedProduct.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("No autorizado");
        }

        preparedProductRepository.delete(preparedProduct);
    }
}
