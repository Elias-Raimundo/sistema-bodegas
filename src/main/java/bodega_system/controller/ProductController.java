package bodega_system.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;

import bodega_system.dto.DashboardStats;
import bodega_system.entity.Company;
import bodega_system.entity.Product;
import bodega_system.repository.CompanyRepository;
import bodega_system.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import bodega_system.entity.Category;
import bodega_system.repository.CategoryRepository;
import bodega_system.dto.ProductDTO;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductRepository productRepository, CompanyRepository companyRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
    }

    @PostMapping
    public Product create(@RequestBody ProductDTO dto, HttpServletRequest request) {

        Long companyId = (Long) request.getAttribute("companyId");
        Company company = companyRepository.findById(companyId).orElseThrow();

        Category category = null;

        if (dto.categoryId != null){
            category = categoryRepository
                .findByIdAndCompany(dto.categoryId, company)
                .orElseThrow(() -> new RuntimeException("Categoria no encontrada"));
        }
        if (dto.name == null || dto.name.trim().isEmpty()){
            throw new RuntimeException("El nombre es obligatorio");
        }

        if (dto.price == null || dto.price < 0){
            throw new RuntimeException("El precio no puede ser negativo");
        }
        if (dto.stock == null || dto.stock <0){
            throw new RuntimeException("El stock no puede ser negativo");
        }

        Product product = new Product();
        product.setName(dto.name.trim());
        product.setPrice(dto.price);
        product.setStock(dto.stock);
        product.setDescripcion(dto.description);
        product.setCompany(company);
        product.setCategory(category);

        return productRepository.save(product);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request){
        Long companyId = (Long) request.getAttribute("companyId");

        Product product = productRepository.findById(id).orElseThrow();

        if(!product.getCompany().getId().equals(companyId)){
            throw new RuntimeException("No autorizado");
        }

        productRepository.delete(product);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id,
                        @RequestBody ProductDTO dto,
                        HttpServletRequest request) {

        Long companyId = (Long) request.getAttribute("companyId");

        Product product = productRepository.findById(id).orElseThrow();

        if (!product.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("No autorizado");
        }
        if (dto.price == null || dto.price < 0){
            throw new RuntimeException("El precio no puede ser negativo");
        }
        if (dto.stock == null ||dto.stock < 0){
            throw new RuntimeException("El stock no puede ser negativo");
        }

        if (dto.name == null || dto.name.trim().isEmpty()){
            throw new RuntimeException("El nombre es obligatorio");
        }

        product.setName(dto.name.trim());
        product.setPrice(dto.price);
        product.setStock(dto.stock);

        if (dto.categoryId != null){
            Category category = categoryRepository
                .findByIdAndCompany(dto.categoryId, product.getCompany())
                .orElseThrow(() -> new RuntimeException("Categoria no encontrada"));
            product.setCategory(category);
        }else{
            product.setCategory(null);
        }
        return productRepository.save(product);
    }

    @GetMapping
    public List<Product> getAll(
            @RequestParam(required = false) Long categoryId,
            HttpServletRequest request) {

        Long companyId = (Long) request.getAttribute("companyId");

        if (categoryId != null)
                return productRepository.findByCompanyIdAndCategoryId(companyId, categoryId);

        return productRepository.findByCompanyId(companyId);
    }

    @GetMapping("/stats")
    public DashboardStats getStats(HttpServletRequest request) {

        Long companyId = (Long) request.getAttribute("companyId");

        var products = productRepository.findByCompanyId(companyId);

        DashboardStats stats = new DashboardStats();

        stats.totalProducts = products.size();

        stats.totalStock = products.stream()
                .mapToDouble(p -> p.getStock())
                .sum();

        stats.lowStock = products.stream()
                .filter(p -> p.getStock() < 5)
                .count();

        stats.inventoryValue = products.stream()
                .mapToDouble(p -> p.getPrice() * Math.floor(p.getStock()))
                .sum();

        return stats;
    }
}
