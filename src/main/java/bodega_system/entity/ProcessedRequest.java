package bodega_system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Registro de operaciones ya procesadas, identificadas por una clave que
// manda el frontend. Sirve para que una venta o una acción de mesa que se
// reenvía al volver internet no se cargue dos veces.
@Entity
@Table(name = "processed_request")
public class ProcessedRequest {

    // companyId + ":" + clave enviada por el frontend
    @Id
    private String requestKey;

    private Long companyId;

    private LocalDateTime createdAt;

    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
