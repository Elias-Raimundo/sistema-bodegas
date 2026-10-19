package bodega_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import bodega_system.entity.ProcessedRequest;

public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, String> {
}
