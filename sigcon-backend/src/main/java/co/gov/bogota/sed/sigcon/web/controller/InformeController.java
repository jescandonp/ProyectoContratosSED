package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeRequest;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.ObservacionRequest;
import co.gov.bogota.sed.sigcon.application.service.InformeEstadoService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/informes")
@Tag(name = "Informes", description = "Gestión de informes de ejecución contractual del Incremento 2")
public class InformeController {

    private final InformeService informeService;
    private final InformeEstadoService informeEstadoService;

    public InformeController(InformeService informeService, InformeEstadoService informeEstadoService) {
        this.informeService = informeService;
        this.informeEstadoService = informeEstadoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Lista informes según el rol autenticado o por contrato")
    public Page<InformeResumenDto> listarInformes(
        @RequestParam(required = false) Long contratoId,
        Pageable pageable,
        Authentication authentication
    ) {
        if (contratoId != null) {
            return informeService.listarPorContrato(contratoId, pageable);
        }
        if (hasRole(authentication, "CONTRATISTA")) {
            return informeService.listarMisInformes(pageable);
        }
        if (hasRole(authentication, "REVISOR")) {
            return informeService.listarParaRevisor(pageable);
        }
        if (hasRole(authentication, "SUPERVISOR")) {
            return informeService.listarParaSupervisor(pageable);
        }
        throw new SigconBusinessException(
            ErrorCode.ACCESO_DENEGADO,
            "Debe consultar informes por contrato",
            HttpStatus.FORBIDDEN
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Obtiene el detalle de un informe")
    public InformeDetalleDto obtenerDetalle(@PathVariable Long id) {
        return informeService.obtenerDetalle(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Crea un informe en borrador")
    public ResponseEntity<InformeDetalleDto> crearInforme(@Valid @RequestBody InformeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(informeService.crearInforme(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Actualiza un informe editable")
    public InformeDetalleDto actualizarInforme(@PathVariable Long id, @Valid @RequestBody InformeRequest request) {
        return informeService.actualizarInforme(id, request);
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Envía un informe para revisión")
    public InformeDetalleDto enviarInforme(@PathVariable Long id, Authentication authentication) {
        return informeEstadoService.enviar(id, authentication.getName());
    }

    @PostMapping("/{id}/aprobar-revision")
    @PreAuthorize("hasRole('REVISOR')")
    @Operation(summary = "Aprueba la revisión y envía el informe a supervisión")
    public InformeDetalleDto aprobarRevision(
        @PathVariable Long id,
        @RequestBody(required = false) ObservacionRequest request,
        Authentication authentication
    ) {
        return informeEstadoService.aprobarRevision(id, authentication.getName(), observacion(request));
    }

    @PostMapping("/{id}/devolver-revision")
    @PreAuthorize("hasRole('REVISOR')")
    @Operation(summary = "Devuelve un informe desde revisión")
    public InformeDetalleDto devolverRevision(
        @PathVariable Long id,
        @Valid @RequestBody ObservacionRequest request,
        Authentication authentication
    ) {
        return informeEstadoService.devolverRevision(id, authentication.getName(), observacion(request));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Aprueba definitivamente un informe")
    public InformeDetalleDto aprobarInforme(@PathVariable Long id, Authentication authentication) {
        return informeEstadoService.aprobar(id, authentication.getName());
    }

    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Devuelve un informe desde supervisión")
    public InformeDetalleDto devolverInforme(
        @PathVariable Long id,
        @Valid @RequestBody ObservacionRequest request,
        Authentication authentication
    ) {
        return informeEstadoService.devolver(id, authentication.getName(), observacion(request));
    }

    private static String observacion(ObservacionRequest request) {
        return request == null ? null : request.getTexto();
    }

    private static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        String authorityName = "ROLE_" + role;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authorityName.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
