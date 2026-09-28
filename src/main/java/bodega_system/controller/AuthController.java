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
import java.security.SecureRandom;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final EmailService emailService;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final int MAX_RESET_ATTEMPTS = 5;

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

        // Respondemos lo mismo exista o no el email, para no revelar
        // qué cuentas están registradas.
        if (userOpt.isEmpty()) {
            return Map.of("message", "Si el email está registrado, te enviamos un código");
        }

        User user = userOpt.get();

        // SecureRandom: generador criptográficamente seguro (Random no lo es)
        String code = String.valueOf(secureRandom.nextInt(900000) + 100000);

        user.setResetCode(code);
        user.setResetCodeExpiry(System.currentTimeMillis() + (5 * 60 * 1000)); // 5 min
        user.setResetAttempts(0);

        userRepository.save(user);

        emailService.sendResetCode(user.getEmail(), code);

        return Map.of("message", "Si el email está registrado, te enviamos un código");
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
            return Map.of("error", "Código inválido o expirado");
        }

        User user = userOpt.get();

        if (
            user.getResetCode() == null ||
            user.getResetCodeExpiry() == null ||
            System.currentTimeMillis() > user.getResetCodeExpiry()
        ) {
            return Map.of("error", "Código inválido o expirado");
        }

        int attempts = user.getResetAttempts() == null ? 0 : user.getResetAttempts();

        if (!user.getResetCode().equals(code)) {
            attempts++;

            // Después de 5 intentos fallidos el código se anula: así no se
            // puede probar el millón de combinaciones por fuerza bruta.
            if (attempts >= MAX_RESET_ATTEMPTS) {
                user.setResetCode(null);
                user.setResetCodeExpiry(null);
                user.setResetAttempts(0);
                userRepository.save(user);
                return Map.of("error", "Demasiados intentos. Pedí un código nuevo");
            }

            user.setResetAttempts(attempts);
            userRepository.save(user);
            return Map.of("error", "Código inválido o expirado");
        }

        user.setPassword(
            encoder.encode(newPassword)
        );

        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        user.setResetAttempts(0);

        userRepository.save(user);

        return Map.of("message", "Contraseña actualizada");
    }
}