package bodega_system.service;

import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import bodega_system.dto.LoginRequest;
import bodega_system.dto.RegisterRequest;
import bodega_system.entity.Company;
import bodega_system.entity.User;
import bodega_system.repository.CompanyRepository;
import bodega_system.repository.UserRepository;
import bodega_system.security.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    public AuthService(
        UserRepository userRepository,
        CompanyRepository companyRepository,
        JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, String> register(RegisterRequest request) {

        if (request.companyName == null || request.companyName.trim().isEmpty()) {
            return Map.of("error", "El nombre de la empresa es obligatorio");
        }

        if (request.email == null || request.email.trim().isEmpty()) {
            return Map.of("error", "El email es obligatorio");
        }

        if (request.password == null || request.password.trim().isEmpty()) {
            return Map.of("error", "La contraseña es obligatoria");
        }

        if (request.password.length() < 6) {
            return Map.of("error", "La contraseña debe tener al menos 6 caracteres");
        }

        String email = request.email.trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            return Map.of("error", "El email ya está registrado");
        }

        Company company = new Company();
        company.setName(request.companyName.trim());
        companyRepository.save(company);

        User user = new User();
        user.setEmail(email);
        user.setPassword(encoder.encode(request.password));
        user.setCompany(company);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), company.getId());

        return Map.of("token", token);
    }

    public Map<String, String> login(LoginRequest request) {

        if (request.email == null || request.email.trim().isEmpty()) {
            return Map.of("error", "El email es obligatorio");
        }

        if (request.password == null || request.password.trim().isEmpty()) {
            return Map.of("error", "La contraseña es obligatoria");
        }

        String email = request.email.trim().toLowerCase();

        User user = userRepository.findByEmail(email)
            .orElse(null);

        if (user == null) {
            return Map.of("error", "Usuario no encontrado");
        }

        if (!encoder.matches(request.password, user.getPassword())) {
            return Map.of("error", "Contraseña incorrecta");
        }

        String token = jwtUtil.generateToken(
            user.getId(),
            user.getCompany().getId()
        );

        return Map.of("token", token);
    }
}