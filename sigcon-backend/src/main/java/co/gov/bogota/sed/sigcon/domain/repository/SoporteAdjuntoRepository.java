package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SoporteAdjuntoRepository extends JpaRepository<SoporteAdjunto, Long> {

    List<SoporteAdjunto> findByActividadIdAndActivoTrue(Long actividadId);

    Optional<SoporteAdjunto> findByIdAndActivoTrue(Long id);
}
