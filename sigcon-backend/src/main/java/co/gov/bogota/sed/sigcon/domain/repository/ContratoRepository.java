package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    Page<Contrato> findByContratistaAndActivoTrue(Usuario contratista, Pageable pageable);

    Page<Contrato> findBySupervisorAndActivoTrue(Usuario supervisor, Pageable pageable);

    Page<Contrato> findByRevisorAndActivoTrue(Usuario revisor, Pageable pageable);

    Page<Contrato> findByActivoTrue(Pageable pageable);

    Optional<Contrato> findByIdAndActivoTrue(Long id);

    Optional<Contrato> findByNumeroAndActivoTrue(String numero);

    /**
     * I7: Búsqueda de contratos por texto libre (numero, objeto, estado, nombre contratista).
     * Limitado a los primeros 50 resultados.
     */
    @Query("SELECT c FROM Contrato c WHERE c.activo = true AND ("
        + "LOWER(c.numero)              LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(c.objeto)              LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(CAST(c.estado AS string)) LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(c.contratista.nombre)  LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(c.contratista.email)   LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Contrato> buscarContratos(@Param("q") String q, Pageable pageable);

    /**
     * T11: Búsqueda de contratos con filtros combinados y paginación.
     * Todos los filtros son opcionales (null = no aplica).
     * Ordenamiento: número de contrato ascendente (el ordenamiento por periodo/estado se aplica en el servicio).
     */
    @Query("SELECT DISTINCT c FROM Contrato c WHERE c.activo = true"
        + " AND (:q IS NULL OR :q = '' OR "
        + "   LOWER(c.numero) LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "   LOWER(c.objeto) LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "   LOWER(CAST(c.estado AS string)) LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "   LOWER(c.contratista.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "   LOWER(c.contratista.email) LIKE LOWER(CONCAT('%', :q, '%')))"
        + " AND (:estadoContrato IS NULL OR c.estado = :estadoContrato)"
        + " AND (:contratistaId IS NULL OR c.contratista.id = :contratistaId)"
        + " AND (:revisorId IS NULL OR c.revisor.id = :revisorId)"
        + " ORDER BY c.numero ASC")
    Page<Contrato> buscarContratosConFiltros(
        @Param("q") String q,
        @Param("estadoContrato") EstadoContrato estadoContrato,
        @Param("contratistaId") Long contratistaId,
        @Param("revisorId") Long revisorId,
        Pageable pageable
    );
}
