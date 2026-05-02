package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InformeRepository extends JpaRepository<Informe, Long> {

    Page<Informe> findByContratoContratistaAndActivoTrue(Usuario contratista, Pageable pageable);

    Page<Informe> findByContratoRevisorAndEstadoAndActivoTrue(Usuario revisor, EstadoInforme estado, Pageable pageable);

    Page<Informe> findByContratoSupervisorAndEstadoAndActivoTrue(Usuario supervisor, EstadoInforme estado, Pageable pageable);

    Page<Informe> findByContratoIdAndActivoTrue(Long contratoId, Pageable pageable);

    Optional<Informe> findByIdAndActivoTrue(Long id);

    Integer countByContratoId(Long contratoId);
}
