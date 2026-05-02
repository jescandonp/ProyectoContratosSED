package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservacionRepository extends JpaRepository<Observacion, Long> {

    List<Observacion> findByInformeIdAndActivoTrueOrderByFechaAsc(Long informeId);
}
