package bodega_system.config;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import bodega_system.entity.ProcessedRequest;
import bodega_system.repository.ProcessedRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Si el frontend manda el header X-Idempotency-Key y esa clave ya se
// procesó con éxito, no se ejecuta de nuevo: se responde {"duplicate": true}.
// Lo usa el modo sin conexión para reenviar operaciones sin duplicarlas.
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Idempotency-Key";
    private static final String ATTR = "idempotencyKey";

    private final ProcessedRequestRepository repository;

    public IdempotencyInterceptor(ProcessedRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String key = request.getHeader(HEADER);
        Object companyId = request.getAttribute("companyId");

        if (key == null || key.isBlank() || companyId == null || "GET".equals(request.getMethod())) {
            return true;
        }

        if (key.length() > 100) {
            key = key.substring(0, 100);
        }

        String fullKey = companyId + ":" + key;

        if (repository.existsById(fullKey)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"duplicate\":true}");
            return false;
        }

        request.setAttribute(ATTR, fullKey);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {

        Object fullKey = request.getAttribute(ATTR);

        // Solo se registra si la operación salió bien (los errores 400 no
        // cuentan, así se pueden reintentar)
        if (fullKey == null || ex != null || response.getStatus() >= 300) {
            return;
        }

        try {
            ProcessedRequest processed = new ProcessedRequest();
            processed.setRequestKey(fullKey.toString());
            processed.setCompanyId((Long) request.getAttribute("companyId"));
            processed.setCreatedAt(LocalDateTime.now());
            repository.save(processed);
        } catch (Exception ignored) {
            // Si ya existía (dos envíos simultáneos), no pasa nada
        }
    }
}
