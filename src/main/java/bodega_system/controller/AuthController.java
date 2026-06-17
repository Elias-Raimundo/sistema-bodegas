package bodega_system.controller;

import org.springframework.web.bind.annotation.*;

import bodega_system.repository.UserRepository;
import bodega_system.service.AuthService;
import bodega_system.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import bodega_system.dto.LoginRequest;
import bodega_system.dto.RegisterRequest;
import bodega_system.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final EmailService emailService;
    private final AuthService authService;

    public AuthController(UserRepository userRepository, 
                            EmailService emailService,
                            AuthService authService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.authService = authService;
    }


    @PostMapping("/register")
    public Map<String, String> register(@RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/test")
    public String test(HttpServletRequest request){
        Long companyId = (Long) request.getAttribute("companyId");
        return "Company ID: " + companyId;
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()){
            return Map.of("error", "El email es obligatorio");
        }
        email = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return Map.of("error", "Usuario no encontrado");
        }

        User user = userOpt.get();

        String code = String.valueOf(new java.util.Random().nextInt(900000) + 100000);

        user.setResetCode(code);
        user.setResetCodeExpiry(System.currentTimeMillis() + (5 * 60 * 1000)); // 5 min

        userRepository.save(user);

        emailService.sendResetCode(user.getEmail(), code);
        
        return Map.of("message", "Codigo enviado al email");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
        @RequestBody Map<String, String> request
    ) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if (email == null || email.trim().isEmpty()) {
            return Map.of("error", "El email es obligatorio");
        }

        if (code == null || code.trim().isEmpty()) {
            return Map.of("error", "El código es obligatorio");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Map.of("error", "La nueva contraseña es obligatoria");
        }

        if (newPassword.length() < 6) {
            return Map.of("error", "La contraseña debe tener al menos 6 caracteres");
        }

        email = email.trim().toLowerCase();
        code = code.trim();

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return Map.of("error", "Usuario no encontrado");
        }

        User user = userOpt.get();

        if (
            user.getResetCode() == null ||
            !user.getResetCode().equals(code)
        ) {
            return Map.of("error", "Código inválido");
        }

        if (
            user.getResetCodeExpiry() == null ||
            System.currentTimeMillis() > user.getResetCodeExpiry()
        ) {
            return Map.of("error", "Código expirado");
        }

        user.setPassword(
            encoder.encode(newPassword)
        );

        user.setResetCode(null);
        user.setResetCodeExpiry(null);

        userRepository.save(user);

        return Map.of("message", "Contraseña actualizada");
    }
}