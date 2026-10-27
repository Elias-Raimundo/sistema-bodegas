package bodega_system.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

import bodega_system.entity.Company;
import bodega_system.repository.CompanyRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/companies")
public class CompanyController {

    // ~1,5 MB en base64. Desde Ajustes el logo ya se achica antes de subirlo,
    // así que en la práctica pesa mucho menos.
    private static final int MAX_LOGO_LENGTH = 2_000_000;

    private final CompanyRepository repository;

    public CompanyController(CompanyRepository repository) {
        this.repository = repository;
    }

    // Respuesta con los datos de la empresa; el logo solo si se pide
    private Map<String, Object> toResponse(Company company, String logo) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", company.getId());
        res.put("name", company.getName());
        res.put("email", company.getEmail());
        res.put("primaryColor", company.getPrimaryColor());
        res.put("darkMode", company.getDarkMode());
        res.put("logo", logo);
        return res;
    }

    @PostMapping("/logo")
    public Map<String, Object> uploadLogo(@RequestBody Map<String, String> request, HttpServletRequest req){
        Long companyId = (Long) req.getAttribute("companyId");
        Company company = repository.findById(companyId).orElseThrow();
        String logo = request.get("logo");
        if (logo == null || logo.trim().isEmpty()){
            throw new RuntimeException("El logo es obligatorio");
        }
        if (logo.length() > MAX_LOGO_LENGTH) {
            throw new RuntimeException("La imagen es demasiado grande. Probá con una más chica.");
        }

        repository.updateLogo(companyId, logo);
        return toResponse(company, logo);
    }

    // ?logo=false para no traer el logo cuando no hace falta
    @GetMapping("/me")
    public Map<String, Object> getMyCompany(
        @RequestParam(name = "logo", defaultValue = "true") boolean withLogo,
        HttpServletRequest req
    ){
        Long companyId = (Long) req.getAttribute("companyId");
        Company company = repository.findById(companyId).orElseThrow();
        return toResponse(company, withLogo ? repository.findLogoById(companyId) : null);
    }

    @PutMapping("/update")
    public Map<String, Object> update (@RequestBody Company updated, HttpServletRequest req){
        Long companyId = (Long) req.getAttribute("companyId");

        Company company = repository.findById(companyId).orElseThrow();

        if (updated.getName() == null || updated.getName().trim().isEmpty()){
            throw new RuntimeException("El nombre de la empresa es obligatorio");
        }
        company.setName(updated.getName().trim());
        company.setPrimaryColor(updated.getPrimaryColor());
        company.setDarkMode(updated.getDarkMode());

        repository.save(company);
        return toResponse(company, repository.findLogoById(companyId));
    }
}
