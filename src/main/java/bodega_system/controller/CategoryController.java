package bodega_system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import bodega_system.entity.Category;
import bodega_system.entity.Company;
import bodega_system.entity.Product;
import bodega_system.repository.CategoryRepository;
import bodega_system.repository.CompanyRepository;
import bodega_system.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @GetMapping
    public List<Category> getAll(HttpServletRequest request){
        Long companyId =
            (Long) request.getAttribute("companyId");
        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();
        return categoryRepository.findByCompany(
            company
        );
    }

    @PostMapping
    public Category create(@RequestBody Category category,HttpServletRequest request){
        Long companyId =
            (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        if (category.getName() == null || category.getName().trim().isEmpty()){
            throw new RuntimeException("La categoria es obligatoria");
        }
        String name = category.getName().trim();

        if(categoryRepository.existsByNameAndCompany(name, company)){
            throw new RuntimeException(
                "La categoría ya existe"
            );
        }
        category.setName(name);
        category.setCompany(company);
        return categoryRepository.save(category);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @RequestBody Category dto, HttpServletRequest request) {
        Long companyId =
            (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        Category category =
            categoryRepository
                .findByIdAndCompany(id, company)
                .orElseThrow();

        if(dto.getName() == null || dto.getName().trim().isEmpty()){
            throw new RuntimeException("La categoria es obligatoria");
        }

        String newName = dto.getName().trim();
        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByNameAndCompany(newName, company)){
            throw new RuntimeException("La categoria ya existe");
        }
        category.setName(newName);
        return categoryRepository.save(category);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request){
        Long companyId =
            (Long) request.getAttribute("companyId");

        Company company =
            companyRepository.findById(companyId)
                .orElseThrow();

        Category category =
            categoryRepository
                .findByIdAndCompany(id, company)
                .orElseThrow();

        List<Product> products =
                productRepository.findByCategoryId(id);
        
        if (!products.isEmpty()){
            throw new RuntimeException("No se puede eliminar una categoria con productos asociados");
        }

        categoryRepository.delete(category);
    }
}
