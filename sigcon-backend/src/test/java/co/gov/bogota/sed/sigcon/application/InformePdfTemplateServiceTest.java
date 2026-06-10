package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.service.InformePdfTemplateService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.AporteSgssiRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InformePdfTemplateServiceTest {

    @Test
    void htmlMantieneFooterFueraDelFlujoSuperiorYFirmasAcotadas() throws Exception {
        String html = buildHtml(informe());

        // El running-footer debe declararse despues del running-header pero ANTES
        // del primer sec-title del flujo, para que Flying Saucer lo extraiga como
        // @bottom-center desde la pagina 1 (si se declara despues solo aparece
        // a partir de la pagina donde fisicamente esta en el flujo).
        assertThat(html.indexOf("class=\"running-footer\""))
            .isGreaterThan(html.indexOf("class=\"running-header\""))
            .isLessThan(html.indexOf("class=\"sec-title\""));
        assertThat(html.indexOf("P&#225;gina <span class=\"page-num\"></span> de <span class=\"page-total\"></span>"))
            .isLessThan(html.indexOf("PERIODO DEL INFORME"));
        assertThat(html).contains("P&#225;gina: <span class=\"page-num\"></span> de <span class=\"page-total\"></span>");
        assertThat(html).contains("style=\"height:44pt;width:150pt;\"");
        assertThat(html).contains("class=\"lbl4\"");
        assertThat(html).contains("Fecha de Terminaci&#243;n:");
        // F2: celdas del header con borde completo
        assertThat(html).contains(".ph-logo{width:14%;text-align:center;padding:2pt;border:0.8pt solid #000");
        // F3: titulo INFORME DE ACTIVIDADES en 14pt
        assertThat(html).contains("font-size:14pt");
        // F4: th con fondo #C0C0C0
        assertThat(html).contains("background:#C0C0C0");
        // F5: dia en letras + numero (fechaElaboracion=2025-02-04 → "cuatro (4)")
        assertThat(html).contains("cuatro (4)");
    }

    @Test
    void htmlContratoTipoOpsMuestraTextoPrestacionServicios() throws Exception {
        Informe informe = informe();
        informe.getContrato().setTipo(TipoContrato.OPS);

        String html = buildHtml(informe);

        assertThat(html).contains("CONTRATO DE PRESTACION DE SERVICIOS PROFESIONALES");
    }

    @Test
    void htmlContratoTipoProMuestraTextoApoyoGestion() throws Exception {
        Informe informe = informe();
        informe.getContrato().setTipo(TipoContrato.PRO);

        String html = buildHtml(informe);

        assertThat(html).contains("CONTRATO DE APOYO A LA GESTION");
        assertThat(html).doesNotContain("CONTRATO DE PRESTACION DE SERVICIOS PROFESIONALES");
    }

    @Test
    void seccion3NoDobleEscapeEntidadesHtmlEnItemSgssi() throws Exception {
        Informe informe = informe();

        AporteSgssi aporte = new AporteSgssi();
        aporte.setId(1L);
        aporte.setItem(co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi.PENSION);
        aporte.setEntidad("Colpensiones");
        aporte.setValorAportado(BigDecimal.valueOf(500000));
        aporte.setFechaPago(LocalDate.of(2025, 1, 10));
        aporte.setActivo(true);

        InformePdfTemplateService service = new InformePdfTemplateService(
            mock(ActividadInformeRepository.class),
            mock(SoporteAdjuntoRepository.class),
            mock(DocumentoAdicionalRepository.class),
            mock(AporteSgssiRepository.class)
        );

        Method buildHtml = InformePdfTemplateService.class.getDeclaredMethod(
            "buildHtml",
            Informe.class, java.util.List.class, java.util.List.class,
            java.util.List.class, byte[].class, byte[].class, byte[].class
        );
        buildHtml.setAccessible(true);

        String html = (String) buildHtml.invoke(
            service, informe,
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(aporte),
            new byte[]{1, 2, 3}, new byte[]{1, 2, 3}, new byte[]{1, 2, 3}
        );

        // La entidad HTML debe aparecer intacta (válida para el parser XHTML)
        assertThat(html).contains("PENSI&#211;N");
        // No debe aparecer el doble escape que produce texto literal en el PDF
        assertThat(html).doesNotContain("PENSI&amp;#211;N");
    }

    private static String buildHtml(Informe informe) throws Exception {
        InformePdfTemplateService service = new InformePdfTemplateService(
            mock(ActividadInformeRepository.class),
            mock(SoporteAdjuntoRepository.class),
            mock(DocumentoAdicionalRepository.class),
            mock(AporteSgssiRepository.class)
        );

        Method buildHtml = InformePdfTemplateService.class.getDeclaredMethod(
            "buildHtml",
            Informe.class,
            java.util.List.class,
            java.util.List.class,
            java.util.List.class,
            byte[].class,
            byte[].class,
            byte[].class
        );
        buildHtml.setAccessible(true);

        return (String) buildHtml.invoke(
            service,
            informe,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            new byte[]{1, 2, 3},
            new byte[]{1, 2, 3},
            new byte[]{1, 2, 3}
        );
    }

    private static Informe informe() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA, "Juan Escandon Perez", "Contratista OPS");
        Usuario supervisor = usuario(4L, RolUsuario.SUPERVISOR, "Supervisor SIGCON", "Supervisor contractual");
        Usuario revisor = usuario(5L, RolUsuario.REVISOR, "Revisor SIGCON", "Apoyo supervision");

        Contrato c = new Contrato();
        c.setId(1L);
        c.setNumero("CO1.PCCNTR 8504408 - 2025");
        c.setObjeto("Prestacion de servicios profesionales de apoyo a la gestion contractual y administrativa de la SED.");
        c.setTipo(TipoContrato.OPS);
        c.setValorTotal(BigDecimal.valueOf(48000000));
        c.setFechaInicio(LocalDate.of(2025, 1, 15));
        c.setFechaFin(LocalDate.of(2025, 12, 31));
        c.setEstado(EstadoContrato.EN_EJECUCION);
        c.setContratista(contratista);
        c.setSupervisor(supervisor);
        c.setRevisor(revisor);
        c.setDependencia("Direccion de Contratacion");
        c.setFormaPago("Pagos mensuales previa presentacion y aprobacion del informe de actividades y soportes requeridos.");
        c.setActivo(true);

        Informe i = new Informe();
        i.setId(2L);
        i.setNumero(2);
        i.setContrato(c);
        i.setFechaInicio(LocalDate.of(2025, 1, 15));
        i.setFechaFin(LocalDate.of(2025, 12, 31));
        i.setFechaElaboracion(LocalDate.of(2025, 2, 4));
        i.setEstado(EstadoInforme.APROBADO);
        i.setActivo(true);
        return i;
    }

    @Test
    void seccion1UsaPlazoCustomSiExiste() throws Exception {
        Informe informe = informe();
        informe.getContrato().setPlazo("El plazo del contrato es de seis (6) meses contados desde la suscripción del acta de inicio.");

        InformePdfTemplateService service = new InformePdfTemplateService(
            mock(ActividadInformeRepository.class),
            mock(SoporteAdjuntoRepository.class),
            mock(DocumentoAdicionalRepository.class),
            mock(AporteSgssiRepository.class)
        );

        java.lang.reflect.Method buildHtml = InformePdfTemplateService.class.getDeclaredMethod(
            "buildHtml",
            Informe.class, java.util.List.class, java.util.List.class,
            java.util.List.class, byte[].class, byte[].class, byte[].class
        );
        buildHtml.setAccessible(true);

        String html = (String) buildHtml.invoke(
            service, informe,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            new byte[]{1}, new byte[]{1}, new byte[]{1}
        );

        assertThat(html).contains("El plazo del contrato es de seis (6) meses");
        // No debe aparecer el texto hardcodeado cuando hay plazo personalizado
        assertThat(html).doesNotContain("ser&#225; hasta el");
    }

    @Test
    void seccion1UsaFallbackSiPlazoNulo() throws Exception {
        Informe informe = informe();
        informe.getContrato().setPlazo(null);

        InformePdfTemplateService service = new InformePdfTemplateService(
            mock(ActividadInformeRepository.class),
            mock(SoporteAdjuntoRepository.class),
            mock(DocumentoAdicionalRepository.class),
            mock(AporteSgssiRepository.class)
        );

        java.lang.reflect.Method buildHtml = InformePdfTemplateService.class.getDeclaredMethod(
            "buildHtml",
            Informe.class, java.util.List.class, java.util.List.class,
            java.util.List.class, byte[].class, byte[].class, byte[].class
        );
        buildHtml.setAccessible(true);

        String html = (String) buildHtml.invoke(
            service, informe,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            new byte[]{1}, new byte[]{1}, new byte[]{1}
        );

        // Texto hardcodeado aparece como fallback
        assertThat(html).contains("ser&#225; hasta el");
    }

    private static Usuario usuario(Long id, RolUsuario rol, String nombre, String cargo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("u" + id + "@educacionbogota.edu.co");
        u.setNombre(nombre);
        u.setCargo(cargo);
        u.setRol(rol);
        u.setFirmaImagen("firmas/" + id + "/firma.png");
        u.setActivo(true);
        return u;
    }
}
