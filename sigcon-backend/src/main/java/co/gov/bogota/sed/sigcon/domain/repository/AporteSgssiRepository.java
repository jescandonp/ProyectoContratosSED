package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AporteSgssiRepository extends JpaRepository<AporteSgssi, Long> {

    List<AporteSgssi> findByInformeIdAndActivoTrue(Long informeId);
}
