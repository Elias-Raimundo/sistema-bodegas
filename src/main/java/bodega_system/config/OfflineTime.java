package bodega_system.config;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.servlet.http.HttpServletRequest;

// Hora de las operaciones hechas sin conexión.
// Cuando el frontend sincroniza algo que se hizo sin internet, manda la hora
// real en el header X-Offline-Created-At, así la venta queda registrada en
// el momento en que se hizo (y cae en la caja y los reportes correctos).
public final class OfflineTime {

    public static final String HEADER = "X-Offline-Created-At";
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private OfflineTime() {}

    public static boolean isOffline(HttpServletRequest request) {
        String value = request.getHeader(HEADER);
        return value != null && !value.isBlank();
    }

    // Devuelve la hora enviada por el frontend si es razonable
    // (de los últimos 7 días y no del futuro); si no, la hora actual.
    public static LocalDateTime resolve(HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        String value = request.getHeader(HEADER);

        if (value == null || value.isBlank()) {
            return now;
        }

        try {
            LocalDateTime sent = LocalDateTime.parse(value.trim());

            if (sent.isAfter(now)) {
                return now;
            }
            if (sent.isBefore(now.minusDays(7))) {
                return now;
            }
            return sent;
        } catch (Exception e) {
            return now;
        }
    }
}
