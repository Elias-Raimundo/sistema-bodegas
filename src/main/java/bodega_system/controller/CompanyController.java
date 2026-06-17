package bodega_system.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

import bodega_system.entity.Company;
import bodega_system.repository.CompanyRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyRepository repository;

    public CompanyController(CompanyRepository repository) {
        this.repository = repository;
    }


    @PostMapping("/logo")
    public Company uploadLogo(@RequestBody Map<String, String> request, HttpServletRequest req){
        Long companyId = (Long) req.getAttribute("companyId");
        Company company = repository.findById(companyId).orElseThrow();
        String logo = request.get("logo");
        if (logo == null || logo.trim().isEmpty()){
            throw new RuntimeException("El logo es obligatorio");
        }

        company.setLogo(logo);
        return repository.save(company);
    }

    @GetMapping("/me")
    public Company getMyCompany( HttpServletRequest req){
        Long companyId = (Long) req.getAttribute("companyId");
        return repository.findById(companyId).orElseThrow();
    }

    @PutMapping("/update")
    public Company update (@RequestBody Company updated, HttpServletRequest req){
        Long companyId = (Long) req.getAttribute("companyId");

        Company company = repository.findById(companyId).orElseThrow();

        if (updated.getName() == null || updated.getName().trim().isEmpty()){
            throw new RuntimeException("El nombre de la empresa es obligatorio");
        }
        company.setName(updated.getName().trim());
        company.setPrimaryColor(updated.getPrimaryColor());
        company.setDarkMode(updated.getDarkMode());

        return repository.save(company);
    }
}
