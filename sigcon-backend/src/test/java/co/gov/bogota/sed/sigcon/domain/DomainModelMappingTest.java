package co.gov.bogota.sed.sigcon.domain;

import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoCatalogo;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoCatalogoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
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

class DomainModelMappingTest {

    @Test
    void mapsEntitiesToI1OracleTablesAndSequences() throws Exception {
        assertEntityMapping(Usuario.class, "SGCN_USUARIOS", "SGCN_USUARIOS_SEQ");
        assertEntityMapping(Contrato.class, "SGCN_CONTRATOS", "SGCN_CONTRATOS_SEQ");
        assertEntityMapping(Obligacion.class, "SGCN_OBLIGACIONES", "SGCN_OBLIGACIONES_SEQ");
        assertEntityMapping(DocumentoCatalogo.class, "SGCN_DOCS_CATALOGO", "SGCN_DOCS_CATALOGO_SEQ");
    }

    @Test
    void mapsContractAndObligationRelationships() throws Exception {
        assertManyToOne(Contrato.class, "contratista", "ID_CONTRATISTA", false);
        assertManyToOne(Contrato.class, "revisor", "ID_REVISOR", true);
        assertManyToOne(Contrato.class, "supervisor", "ID_SUPERVISOR", true);
        assertManyToOne(Obligacion.class, "contrato", "ID_CONTRATO", false);
    }

    @Test
    void exposesRequiredRepositoryMethods() throws Exception {
        assertThat(UsuarioRepository.class.getMethod("findByEmailAndActivoTrue", String.class).getReturnType())
            .isEqualTo(Optional.class);
        assertThat(UsuarioRepository.class.getMethod("findByRolAndActivoTrue", RolUsuario.class).getReturnType())
            .isEqualTo(List.class);
        assertThat(UsuarioRepository.class.getMethod("findByActivoTrue", Pageable.class).getReturnType())
            .isEqualTo(Page.class);

        assertThat(ContratoRepository.class.getMethod("findByContratistaAndActivoTrue", Usuario.class, Pageable.class).getReturnType())
            .isEqualTo(Page.class);
        assertThat(ContratoRepository.class.getMethod("findBySupervisorAndActivoTrue", Usuario.class, Pageable.class).getReturnType())
            .isEqualTo(Page.class);
        assertThat(ContratoRepository.class.getMethod("findByActivoTrue", Pageable.class).getReturnType())
            .isEqualTo(Page.class);
        assertThat(ContratoRepository.class.getMethod("findByNumeroAndActivoTrue", String.class).getReturnType())
            .isEqualTo(Optional.class);

        assertThat(ObligacionRepository.class.getMethod("findByContratoIdAndActivoTrueOrderByOrdenAsc", Long.class).getReturnType())
            .isEqualTo(List.class);

        assertThat(DocumentoCatalogoRepository.class.getMethod("findByTipoContratoAndActivoTrue", TipoContrato.class).getReturnType())
            .isEqualTo(List.class);
        assertThat(DocumentoCatalogoRepository.class.getMethod("findByActivoTrue", Pageable.class).getReturnType())
            .isEqualTo(Page.class);
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
