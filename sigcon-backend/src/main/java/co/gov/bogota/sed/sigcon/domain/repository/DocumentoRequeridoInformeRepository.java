package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.DocumentoRequeridoInforme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoRequeridoInformeRepository extends JpaRepository<DocumentoRequeridoInforme, Long> {

    List<DocumentoRequeridoInforme> findByInformeIdAndActivoTrue(Long informeId);

    Optional<DocumentoRequeridoInforme> findByInformeIdAndClaveLogicaAndActivoTrue(Long informeId, String claveLogica);

    Optional<DocumentoRequeridoInforme> findByIdAndActivoTrue(Long id);

    boolean existsByInformeIdAndClaveLogicaAndStoragePathIsNotNullAndActivoTrue(Long informeId, String claveLogica);
}
