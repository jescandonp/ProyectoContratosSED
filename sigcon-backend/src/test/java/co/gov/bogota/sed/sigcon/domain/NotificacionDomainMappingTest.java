package co.gov.bogota.sed.sigcon.domain;

import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Notificacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import co.gov.bogota.sed.sigcon.domain.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el mapeo JPA de Notificacion e integridad del repositorio I3
 * sin levantar contexto Spring ni BD Oracle.
 */
class NotificacionDomainMappingTest {

    @Test
    void mapsNotificacionToOracleTableAndSequence() throws Exception {
        assertThat(Notificacion.class.getAnnotation(Entity.class)).isNotNull();
        assertThat(Notificacion.class.getAnnotation(Table.class).name())
            .isEqualTo("SGCN_NOTIFICACIONES");

        Field idField = Notificacion.class.getDeclaredField("id");
        SequenceGenerator sg = idField.getAnnotation(SequenceGenerator.class);
        assertThat(sg.sequenceName()).isEqualTo("SGCN_NOTIFICACIONES_SEQ");
        assertThat(sg.allocationSize()).isEqualTo(1);
    }

    @Test
    void usuarioFkIsNonNullable() throws Exception {
        Field usuarioField = Notificacion.class.getDeclaredField("usuario");
        assertThat(usuarioField.getAnnotation(ManyToOne.class)).isNotNull();
        JoinColumn joinColumn = usuarioField.getAnnotation(JoinColumn.class);
        assertThat(joinColumn.name()).isEqualTo("ID_USUARIO");
        assertThat(joinColumn.nullable()).isFalse();
    }

    @Test
    void informeFkIsNullable() throws Exception {
        Field informeField = Notificacion.class.getDeclaredField("informe");
        assertThat(informeField.getAnnotation(ManyToOne.class)).isNotNull();
        // JoinColumn sin nullable=false => nullable=true por defecto
        JoinColumn joinColumn = informeField.getAnnotation(JoinColumn.class);
        assertThat(joinColumn.name()).isEqualTo("ID_INFORME");
        // Notificacion sin informe debe ser posible en I3+
        Notificacion n = new Notificacion();
        assertThat(n.getInforme()).isNull();
    }

    @Test
    void tipoEventoEnumCoversFiveValues() {
        TipoEvento[] values = TipoEvento.values();
        assertThat(values).hasSize(5);
        assertThat(values).containsExactlyInAnyOrder(
            TipoEvento.INFORME_ENVIADO,
            TipoEvento.REVISION_APROBADA,
            TipoEvento.REVISION_DEVUELTA,
            TipoEvento.INFORME_APROBADO,
            TipoEvento.INFORME_DEVUELTO
        );
    }

    @Test
    void informeExposesPdfMetadataFields() {
        Informe informe = new Informe();
        assertThat(informe.getPdfGeneradoAt()).isNull();
        assertThat(informe.getPdfHash()).isNull();
        // Campos deben seguir siendo nullables (inmutabilidad: solo se setean una vez)
        informe.setPdfHash("abc123");
        assertThat(informe.getPdfHash()).isEqualTo("abc123");
    }

    @Test
    void notificacionRepositoryExposesRequiredMethods() throws Exception {
        assertThat(NotificacionRepository.class
            .getMethod("countByUsuarioAndLeidaFalse", Usuario.class)
            .getReturnType())
            .isEqualTo(long.class);

        assertThat(NotificacionRepository.class
            .getMethod("findByUsuarioOrderByFechaDesc", Usuario.class, Pageable.class)
            .getReturnType())
            .isEqualTo(Page.class);

        assertThat(NotificacionRepository.class
            .getMethod("findByIdAndUsuario", Long.class, Usuario.class)
            .getReturnType())
            .isEqualTo(Optional.class);
    }
}
