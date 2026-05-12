package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InformeRepository extends JpaRepository<Informe, Long> {

    Page<Informe> findByContratoContratistaAndActivoTrue(Usuario contratista, Pageable pageable);

    Page<Informe> findByContratoRevisorAndEstadoAndActivoTrue(Usuario revisor, EstadoInforme estado, Pageable pageable);

    Page<Informe> findByContratoSupervisorAndEstadoAndActivoTrue(Usuario supervisor, EstadoInforme estado, Pageable pageable);

    Page<Informe> findByContratoIdAndActivoTrue(Long contratoId, Pageable pageable);

    Optional<Informe> findByIdAndActivoTrue(Long id);

    Integer countByContratoId(Long contratoId);

    /**
     * I7: Búsqueda de informes por texto libre + rango de periodo.
     * El informe cruza el rango si: informe.fechaInicio <= fechaFin AND informe.fechaFin >= fechaInicio.
     * Si fechaInicio o fechaFin son null, no se aplica ese filtro de rango.
     * Limitado a los primeros 50 resultados.
     */
    @Query("SELECT i FROM Informe i WHERE i.activo = true AND ("
        + "LOWER(CAST(i.numero AS string))          LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(CAST(i.estado AS string))           LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(i.contrato.numero)                  LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(i.contrato.contratista.nombre)      LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(i.contrato.contratista.email)       LIKE LOWER(CONCAT('%', :q, '%'))"
        + ") AND (:fechaInicio IS NULL OR i.fechaFin   >= :fechaInicio)"
        + "  AND (:fechaFin    IS NULL OR i.fechaInicio <= :fechaFin)")
    List<Informe> buscarInformes(
        @Param("q") String q,
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin,
        Pageable pageable
    );
}
