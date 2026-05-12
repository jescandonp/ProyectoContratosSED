package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminResponse;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.ContratistaResultadoDto;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.ContratoResultadoDto;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.InformeResultadoDto;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * I7: Búsqueda administrativa global por texto libre + rango de periodo de informe.
 * Solo accesible para ADMIN (validado en el controller con @PreAuthorize).
 */
@Service
@Transactional(readOnly = true)
public class BusquedaAdminService {

    /** Límite de resultados por grupo para evitar respuestas masivas. */
    private static final int MAX_RESULTADOS = 50;

    private final UsuarioRepository usuarioRepository;
    private final ContratoRepository contratoRepository;
    private final InformeRepository informeRepository;

    public BusquedaAdminService(
        UsuarioRepository usuarioRepository,
        ContratoRepository contratoRepository,
        InformeRepository informeRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.contratoRepository = contratoRepository;
        this.informeRepository = informeRepository;
    }

    /**
     * Ejecuta la búsqueda global.
     *
     * @param q           texto libre (puede ser vacío — retorna todos hasta el límite)
     * @param fechaInicio inicio del rango de periodo de informe (opcional)
     * @param fechaFin    fin del rango de periodo de informe (opcional)
     */
    public BusquedaAdminResponse buscar(String q, LocalDate fechaInicio, LocalDate fechaFin) {
        String termino = q != null ? q.trim() : "";
        Pageable limite = PageRequest.of(0, MAX_RESULTADOS);

        List<ContratistaResultadoDto> contratistas = buscarContratistas(termino, limite);
        List<ContratoResultadoDto> contratos = buscarContratos(termino, limite);
        List<InformeResultadoDto> informes = buscarInformes(termino, fechaInicio, fechaFin, limite);

        return new BusquedaAdminResponse(contratistas, contratos, informes);
    }

    private List<ContratistaResultadoDto> buscarContratistas(String q, Pageable pageable) {
        List<Usuario> usuarios = usuarioRepository.buscarContratistas(q, pageable);
        return usuarios.stream()
            .map(u -> new ContratistaResultadoDto(u.getId(), u.getNombre(), u.getEmail(), u.getCargo()))
            .collect(Collectors.toList());
    }

    private List<ContratoResultadoDto> buscarContratos(String q, Pageable pageable) {
        List<Contrato> contratos = contratoRepository.buscarContratos(q, pageable);
        return contratos.stream()
            .map(c -> new ContratoResultadoDto(
                c.getId(),
                c.getNumero(),
                c.getObjeto(),
                c.getEstado() != null ? c.getEstado().name() : null,
                c.getContratista() != null ? c.getContratista().getNombre() : null
            ))
            .collect(Collectors.toList());
    }

    private List<InformeResultadoDto> buscarInformes(String q, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable) {
        List<Informe> informes = informeRepository.buscarInformes(q, fechaInicio, fechaFin, pageable);
        return informes.stream()
            .map(i -> new InformeResultadoDto(
                i.getId(),
                i.getNumero(),
                i.getEstado() != null ? i.getEstado().name() : null,
                i.getFechaInicio() != null ? i.getFechaInicio().toString() : null,
                i.getFechaFin() != null ? i.getFechaFin().toString() : null,
                i.getContrato() != null ? i.getContrato().getNumero() : null,
                i.getContrato() != null && i.getContrato().getContratista() != null
                    ? i.getContrato().getContratista().getNombre() : null
            ))
            .collect(Collectors.toList());
    }
}
