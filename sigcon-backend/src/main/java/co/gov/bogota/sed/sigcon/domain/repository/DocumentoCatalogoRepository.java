package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.DocumentoCatalogo;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoCatalogoRepository extends JpaRepository<DocumentoCatalogo, Long> {

    List<DocumentoCatalogo> findByTipoContratoAndActivoTrue(TipoContrato tipo);

    Page<DocumentoCatalogo> findByActivoTrue(Pageable pageable);
}
