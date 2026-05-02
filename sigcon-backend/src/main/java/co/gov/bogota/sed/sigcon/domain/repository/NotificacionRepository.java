package co.gov.bogota.sed.sigcon.domain.repository;

import co.gov.bogota.sed.sigcon.domain.entity.Notificacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** Para el badge: cuenta no leidas del usuario autenticado. */
    long countByUsuarioAndLeidaFalse(Usuario usuario);

    /** Centro de notificaciones paginado, mas recientes primero. */
    Page<Notificacion> findByUsuarioOrderByFechaDesc(Usuario usuario, Pageable pageable);

    /** Para marcar como leida verificando propiedad. */
    Optional<Notificacion> findByIdAndUsuario(Long id, Usuario usuario);
}
