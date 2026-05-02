package co.gov.bogota.sed.sigcon.domain;

import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;
import co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObservacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InformeDomainMappingTest {

    @Test
    void mapsEntitiesToI2OracleTablesAndSequences() throws Exception {
        assertEntityMapping(Informe.class, "SGCN_INFORMES", "SGCN_INFORMES_SEQ");
        assertEntityMapping(ActividadInforme.class, "SGCN_ACTIVIDADES", "SGCN_ACTIVIDADES_SEQ");
        assertEntityMapping(SoporteAdjunto.class, "SGCN_SOPORTES", "SGCN_SOPORTES_SEQ");
        assertEntityMapping(DocumentoAdicional.class, "SGCN_DOCS_ADICIONALES", "SGCN_DOCS_ADICIONALES_SEQ");
        assertEntityMapping(Observacion.class, "SGCN_OBSERVACIONES", "SGCN_OBSERVACIONES_SEQ");
    }

    @Test
    void mapsRelationshipsBetweenInformeEntities() throws Exception {
        assertManyToOne(Informe.class, "contrato", "ID_CONTRATO", false);
        assertManyToOne(ActividadInforme.class, "informe", "ID_INFORME", false);
        assertManyToOne(ActividadInforme.class, "obligacion", "ID_OBLIGACION", false);
        assertManyToOne(SoporteAdjunto.class, "actividad", "ID_ACTIVIDAD", false);
        assertManyToOne(DocumentoAdicional.class, "informe", "ID_INFORME", false);
        assertManyToOne(DocumentoAdicional.class, "catalogo", "ID_CATALOGO", false);
        assertManyToOne(Observacion.class, "informe", "ID_INFORME", false);
    }

    @Test
    void coversInformeStateMachineEnums() {
        assertThat(EstadoInforme.values())
            .containsExactly(EstadoInforme.BORRADOR, EstadoInforme.ENVIADO, EstadoInforme.EN_REVISION,
                EstadoInforme.DEVUELTO, EstadoInforme.APROBADO);
        assertThat(TipoSoporte.values()).containsExactly(TipoSoporte.ARCHIVO, TipoSoporte.URL);
        assertThat(RolObservacion.values()).containsExactly(RolObservacion.REVISOR, RolObservacion.SUPERVISOR);
    }

    @Test
    void exposesRequiredI2RepositoryMethods() throws Exception {
        assertThat(InformeRepository.class.getMethod("findByContratoContratistaAndActivoTrue", Usuario.class, Pageable.class).getReturnType())
            .isEqualTo(Page.class);
        assertThat(InformeRepository.class.getMethod("findByContratoRevisorAndEstadoAndActivoTrue", Usuario.class, EstadoInforme.class, Pageable.class).getReturnType())
            .isEqualTo(Page.class);
        assertThat(InformeRepository.class.getMethod("findByContratoSupervisorAndEstadoAndActivoTrue", Usuario.class, EstadoInforme.class, Pageable.class).getReturnType())
            .isEqualTo(Page.class);
        assertThat(InformeRepository.class.getMethod("findByContratoIdAndActivoTrue", Long.class, Pageable.class).getReturnType())
            .isEqualTo(Page.class);
        assertThat(InformeRepository.class.getMethod("findByIdAndActivoTrue", Long.class).getReturnType())
            .isEqualTo(Optional.class);
        assertThat(InformeRepository.class.getMethod("countByContratoId", Long.class).getReturnType())
            .isEqualTo(Integer.class);

        assertThat(ActividadInformeRepository.class.getMethod("findByInformeIdAndActivoTrue", Long.class).getReturnType())
            .isEqualTo(List.class);
        assertThat(SoporteAdjuntoRepository.class.getMethod("findByActividadIdAndActivoTrue", Long.class).getReturnType())
            .isEqualTo(List.class);
        assertThat(DocumentoAdicionalRepository.class.getMethod("findByInformeIdAndActivoTrue", Long.class).getReturnType())
            .isEqualTo(List.class);
        assertThat(ObservacionRepository.class.getMethod("findByInformeIdAndActivoTrueOrderByFechaAsc", Long.class).getReturnType())
            .isEqualTo(List.class);
    }

    @Test
    void informeKeepsPdfRutaNullableAsI3Seam() throws Exception {
        Informe informe = new Informe();
        assertThat(informe.getPdfRuta()).isNull();
        informe.setPdfRuta("ruta/pdf.pdf");
        assertThat(informe.getPdfRuta()).isEqualTo("ruta/pdf.pdf");

        Field pdfField = Informe.class.getDeclaredField("pdfRuta");
        assertThat(pdfField.getAnnotation(javax.persistence.Column.class).nullable()).isTrue();
    }

    private static void assertEntityMapping(Class<?> type, String tableName, String sequenceName) throws Exception {
        assertThat(type.getAnnotation(Entity.class)).isNotNull();
        assertThat(type.getAnnotation(Table.class).name()).isEqualTo(tableName);

        Field id = type.getDeclaredField("id");
        assertThat(id.getAnnotation(SequenceGenerator.class).sequenceName()).isEqualTo(sequenceName);
        assertThat(id.getAnnotation(SequenceGenerator.class).allocationSize()).isEqualTo(1);
    }

    private static void assertManyToOne(Class<?> type, String fieldName, String columnName, boolean nullable) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        assertThat(field.getAnnotation(ManyToOne.class)).isNotNull();
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isEqualTo(nullable);
    }
}
