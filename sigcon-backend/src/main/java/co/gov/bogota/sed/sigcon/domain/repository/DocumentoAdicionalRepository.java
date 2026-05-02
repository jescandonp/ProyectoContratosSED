package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoAdicionalRepository extends JpaRepository<DocumentoAdicional, Long> {

    List<DocumentoAdicional> findByInformeIdAndActivoTrue(Long informeId);

    Optional<DocumentoAdicional> findByIdAndActivoTrue(Long id);
}
