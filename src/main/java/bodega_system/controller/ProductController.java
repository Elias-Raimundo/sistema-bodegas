package bodega_system.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "50") int limit,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        List<Product> products;

        if (search != null && !search.trim().isEmpty()) {
            // Búsqueda por nombre — trae todos los que coincidan
            products = productRepository
                .findByCompany_IdAndNameContainingIgnoreCase(
                    companyId, search.trim()
                );
        } else if (categoryId != null) {
            products = productRepository
                .findByCompanyIdAndCategoryId(companyId, categoryId);
        } else {
            // Sin búsqueda — trae los primeros N
            products = productRepository
                .findByCompanyId(companyId)
                .stream()
                .limit(limit)
                .toList();
        }

        return products;
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

   @PostMapping("/import")
    public String importProducts(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {

        Long companyId = (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        int created = 0;
        int updated = 0;

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    file.getInputStream(),
                    StandardCharsets.UTF_8
                )
            )
        ) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split("[,;]", -1);

                if (data.length < 4) {
                    throw new RuntimeException(
                        "Fila inválida: " + line
                    );
                }

                String name = data[0].trim();
                Double price = Double.parseDouble(data[1].trim());
                Double stockToAdd = Double.parseDouble(data[2].trim());
                String categoryName = data[3].trim();
                String description =
                    data.length > 4 ? data[4].trim() : "";

                if (name.isEmpty()) {
                    throw new RuntimeException(
                        "Producto sin nombre"
                    );
                }

                if (price < 0 || stockToAdd < 0) {
                    throw new RuntimeException(
                        "Precio o stock inválido en: " + name
                    );
                }

                Category category = null;

                if (!categoryName.isEmpty()) {

                    category = categoryRepository
                        .findByNameIgnoreCaseAndCompany(
                            categoryName,
                            company
                        )
                        .orElseGet(() -> {

                            Category newCategory =
                                new Category();

                            newCategory.setName(categoryName);
                            newCategory.setCompany(company);

                            return categoryRepository
                                .save(newCategory);
                        });
                }

                var existingProduct =
                    productRepository
                        .findByNameIgnoreCaseAndCompanyId(
                            name,
                            companyId
                        );

                Product product;

                if (existingProduct.isPresent()) {

                    product = existingProduct.get();

                    // SUMA STOCK
                    product.setStock(
                        product.getStock() + stockToAdd
                    );

                    // ACTUALIZA PRECIO
                    product.setPrice(price);

                    // ACTUALIZA CATEGORÍA
                    product.setCategory(category);

                    // ACTUALIZA DESCRIPCIÓN SOLO SI VIENE
                    if (!description.isEmpty()) {
                        product.setDescripcion(description);
                    }

                    updated++;

                } else {

                    product = new Product();

                    product.setName(name);
                    product.setPrice(price);
                    product.setStock(stockToAdd);
                    product.setCompany(company);
                    product.setCategory(category);
                    product.setDescripcion(description);

                    created++;
                }

                productRepository.save(product);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                "Error importando productos: "
                + e.getMessage()
            );
        }

        return "Productos creados: "
            + created
            + " | Productos actualizados: "
            + updated;
    }
}
