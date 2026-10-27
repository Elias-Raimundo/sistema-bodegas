package bodega_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import bodega_system.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long>{

    // El logo se lee y se guarda aparte, solo cuando hace falta.
    // Antes era un campo de Company y, como casi todas las tablas están
    // vinculadas a la empresa, cada consulta (productos, ventas, mesas...)
    // traía el logo completo desde la base: eso disparaba el consumo de
    // ancho de banda de Supabase.

    @Query(value = "SELECT logo FROM company WHERE id = :id", nativeQuery = true)
    String findLogoById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE company SET logo = :logo WHERE id = :id", nativeQuery = true)
    int updateLogo(@Param("id") Long id, @Param("logo") String logo);
}
