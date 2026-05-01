package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    Page<Contrato> findByContratistaAndActivoTrue(Usuario contratista, Pageable pageable);

    Page<Contrato> findBySupervisorAndActivoTrue(Usuario supervisor, Pageable pageable);

    Page<Contrato> findByRevisorAndActivoTrue(Usuario revisor, Pageable pageable);

    Page<Contrato> findByActivoTrue(Pageable pageable);

    Optional<Contrato> findByIdAndActivoTrue(Long id);

    Optional<Contrato> findByNumeroAndActivoTrue(String numero);
}
