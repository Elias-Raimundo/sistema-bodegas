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
import bodega_system.repository.PreparedProductRepository;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final PreparedProductRepository preparedProductRepository;

    public ProductController(
        ProductRepository productRepository,
        CompanyRepository companyRepository,
        CategoryRepository categoryRepository,
        PreparedProductRepository preparedProductRepository
    ) {
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.categoryRepository = categoryRepository;
        this.preparedProductRepository = preparedProductRepository;
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
        product.setCostPrice(dto.costPrice != null ? dto.costPrice : 0.0);
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

        boolean usedAsIngredient = preparedProductRepository
            .findByCompany(product.getCompany())
            .stream()
            .anyMatch(pp ->
                pp.getIngredients() != null &&
                pp.getIngredients().stream()
                    .anyMatch(ing ->
                        ing.getProduct() != null &&
                        ing.getProduct().getId().equals(id)
                    )
            );

        if (usedAsIngredient) {
            throw new RuntimeException(
                "No se puede eliminar: el producto se usa como ingrediente de un preparado"
            );
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
        product.setCostPrice(dto.costPrice != null ? dto.costPrice : 0.0);
        product.setStock(dto.stock);
        // Solo se pisa si viene en el pedido: el formulario de edición no tiene
        // ese campo, y así no se borran las descripciones cargadas por CSV.
        if (dto.description != null) {
            product.setDescripcion(dto.description);
        }

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
        @RequestParam(required = false) Boolean lowStock,   
        @RequestParam(defaultValue = "1000") int limit,
        HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute("companyId");

        if (Boolean.TRUE.equals(lowStock)) {
            var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
            return productRepository.findByCompanyIdAndStockLessThanOrderByStockAsc(companyId, 2.0, pageable);
        }

        
        if (search != null && !search.trim().isEmpty() && categoryId != null) {
            return productRepository
                .findByCompany_IdAndNameContainingIgnoreCaseAndCategory_Id(
                    companyId, search.trim(), categoryId
                );
        }

        if (search != null && !search.trim().isEmpty()) {
            return productRepository
                .findByCompany_IdAndNameContainingIgnoreCase(
                    companyId, search.trim()
                );
        }

        if (categoryId != null) {
            return productRepository
                .findByCompanyIdAndCategoryId(companyId, categoryId);
        }

        return productRepository
            .findByCompanyId(companyId)
            .stream()
            .limit(limit)
            .toList();
    }

    @GetMapping("/stats")
    public DashboardStats getStats(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute("companyId");

        List<Object[]> rows = productRepository.getInventorySummary(companyId);
        Object[] row = rows.get(0);
        long lowStockCount = productRepository.countByCompanyIdAndStockLessThan(companyId, 2.0);

        DashboardStats stats = new DashboardStats();
        stats.totalProducts = ((Number) row[0]).longValue();
        stats.totalStock = ((Number) row[1]).doubleValue();
        stats.lowStock = lowStockCount;
        stats.inventoryValue = ((Number) row[2]).doubleValue();
        stats.costValue = ((Number) row[3]).doubleValue();

        return stats;
    }

    // Convierte "1500", "1500.5", "1500,50" o "1.500,50" en número.
    private Double parseNumber(String raw, int lineNumber, String field) {
        String v = raw.trim().replace("\"", "").replace("$", "").replace(" ", "");
        if (v.contains(",")) {
            // Formato argentino: el punto es de miles y la coma es decimal
            v = v.replace(".", "").replace(",", ".");
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                "Fila " + lineNumber + ": el " + field + " \"" + raw.trim() + "\" no es un número válido"
            );
        }
    }

    // Todo o nada: si una fila falla, no se guarda ninguna.
    @org.springframework.transaction.annotation.Transactional
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
            String separator = null;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // La primera fila es el encabezado: la usamos para detectar
                // si el archivo separa columnas con ";" (Excel en español) o con ","
                if (separator == null) {
                    separator = line.contains(";") ? ";" : ",";
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] data = line.split(separator, -1);

                if (data.length < 4) {
                    throw new RuntimeException(
                        "Fila " + lineNumber + " inválida: " + line
                    );
                }

                String name = data[0].trim().replace("\"", "");
                Double price = parseNumber(data[1], lineNumber, "precio");
                Double stockToAdd = parseNumber(data[2], lineNumber, "stock");
                String categoryName = data[3].trim().replace("\"", "");
                String description =
                    data.length > 4 ? data[4].trim().replace("\"", "") : "";

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
