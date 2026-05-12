package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailAndActivoTrue(String email);

    Optional<Usuario> findByIdAndActivoTrue(Long id);

    List<Usuario> findByRolAndActivoTrue(RolUsuario rol);

    Page<Usuario> findByActivoTrue(Pageable pageable);

    /**
     * I7: Búsqueda de contratistas por texto libre (nombre, email, cargo).
     * Limitado a los primeros 50 resultados para evitar respuestas masivas.
     */
    @Query("SELECT u FROM Usuario u WHERE u.activo = true AND u.rol = 'CONTRATISTA' AND ("
        + "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(u.email)  LIKE LOWER(CONCAT('%', :q, '%')) OR "
        + "LOWER(u.cargo)  LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Usuario> buscarContratistas(@Param("q") String q, Pageable pageable);
}
