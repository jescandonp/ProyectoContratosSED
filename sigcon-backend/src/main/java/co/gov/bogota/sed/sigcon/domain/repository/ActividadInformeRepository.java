package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActividadInformeRepository extends JpaRepository<ActividadInforme, Long> {

    List<ActividadInforme> findByInformeIdAndActivoTrue(Long informeId);

    Optional<ActividadInforme> findByIdAndActivoTrue(Long id);

    Integer countByInformeIdAndActivoTrue(Long informeId);
}
