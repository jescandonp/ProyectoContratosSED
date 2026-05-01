package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObligacionRepository extends JpaRepository<Obligacion, Long> {

    List<Obligacion> findByContratoIdAndActivoTrueOrderByOrdenAsc(Long contratoId);
}
