package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminFiltros;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminPageResponse;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminResponse;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.ContratistaResultadoDto;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.ContratoResultadoDto;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.InformeResultadoDto;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * I7/T11: Búsqueda administrativa global con filtros combinados, paginación y ordenamiento.
 * Solo accesible para ADMIN (validado en el controller con @PreAuthorize).
 *
 * <p>Ordenamiento default (spec §0.2.1):</p>
 * <ol>
 *   <li>Periodo de informe más reciente primero.</li>
 *   <li>Prioridad operativa de estado: EN_REVISION, ENVIADO, DEVUELTO, BORRADOR, APROBADO.</li>
 *   <li>Número de contrato ascendente.</li>
 *   <li>Contratista ascendente.</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class BusquedaAdminService {

    /** Límite de resultados por grupo para la búsqueda simple (T8 legacy). */
    private static final int MAX_RESULTADOS = 50;

    /** Prioridad operativa de estado de informe (menor = más prioritario). */
    private static final java.util.Map<EstadoInforme, Integer> PRIORIDAD_ESTADO;
    static {
        PRIORIDAD_ESTADO = new java.util.EnumMap<>(EstadoInforme.class);
        PRIORIDAD_ESTADO.put(EstadoInforme.EN_REVISION, 1);
        PRIORIDAD_ESTADO.put(EstadoInforme.EN_VISTO_BUENO, 2);
        PRIORIDAD_ESTADO.put(EstadoInforme.ENVIADO,     3);
        PRIORIDAD_ESTADO.put(EstadoInforme.DEVUELTO,    4);
        PRIORIDAD_ESTADO.put(EstadoInforme.BORRADOR,    5);
        PRIORIDAD_ESTADO.put(EstadoInforme.APROBADO,    6);
    }

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

    // ── API legacy T8 (mantiene compatibilidad con tests existentes) ──────────

    /**
     * Búsqueda simple T8: texto libre + rango de fechas, sin paginación avanzada.
     * Retorna grupos separados de contratistas, contratos e informes.
     */
    public BusquedaAdminResponse buscar(String q, LocalDate fechaInicio, LocalDate fechaFin) {
        String termino = q != null ? q.trim() : "";
        Pageable limite = PageRequest.of(0, MAX_RESULTADOS);

        List<ContratistaResultadoDto> contratistas = buscarContratistas(termino, limite);
        List<ContratoResultadoDto> contratos = buscarContratosSimple(termino, limite);
        List<InformeResultadoDto> informes = buscarInformesSimple(termino, fechaInicio, fechaFin, limite);

        return new BusquedaAdminResponse(contratistas, contratos, informes);
    }

    // ── API T11: filtros combinados + paginación ──────────────────────────────

    /**
     * Búsqueda avanzada T11: filtros combinados, paginación de 20 y ordenamiento default.
     * Retorna contratos con sus informes anidados que cumplen los filtros.
     */
    public BusquedaAdminPageResponse buscarConFiltros(BusquedaAdminFiltros filtros) {
        String q = filtros.getQ() != null ? filtros.getQ().trim() : "";
        int tamano = filtros.getTamano() > 0 ? filtros.getTamano() : 20;
        int pagina = filtros.getPagina() >= 0 ? filtros.getPagina() : 0;

        // Buscar contratos con filtros combinados (paginado)
        Pageable pageRequest = PageRequest.of(pagina, tamano);
        Page<Contrato> paginaContratos = contratoRepository.buscarContratosConFiltros(
            q,
            filtros.getEstadoContrato(),
            filtros.getContratistaId(),
            filtros.getRevisorId(),
            pageRequest
        );

        // Para cada contrato, buscar sus informes que cumplen los filtros de informe
        List<ContratoResultadoDto> resultados = new ArrayList<>();
        for (Contrato contrato : paginaContratos.getContent()) {
            List<Informe> informesContrato = informeRepository.buscarInformesPorContrato(
                contrato.getId(),
                q,
                filtros.getFechaInicio(),
                filtros.getFechaFin(),
                filtros.getEstadoInforme(),
                filtros.getRevisorId()
            );

            // Si hay filtros de informe y no hay informes que los cumplan, incluir el contrato
            // solo si no hay filtros de informe activos (para mostrar contratos sin informes)
            boolean hayFiltrosInforme = filtros.getFechaInicio() != null
                || filtros.getFechaFin() != null
                || filtros.getEstadoInforme() != null
                || filtros.getRevisorId() != null;

            if (hayFiltrosInforme && informesContrato.isEmpty()) {
                // El contrato no tiene informes que cumplan los filtros de informe — omitir
                continue;
            }

            // Ordenar informes: periodo más reciente primero, luego prioridad operativa de estado
            List<Informe> informesOrdenados = informesContrato.stream()
                .sorted(Comparator
                    .comparing(Informe::getFechaFin, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(i -> PRIORIDAD_ESTADO.getOrDefault(i.getEstado(), 99))
                )
                .collect(Collectors.toList());

            ContratoResultadoDto dto = toContratoDto(contrato);
            dto.setInformes(informesOrdenados.stream()
                .map(this::toInformeDto)
                .collect(Collectors.toList()));
            resultados.add(dto);
        }

        // Ordenar contratos: contratista ascendente (número ya viene ordenado del repositorio)
        resultados.sort(Comparator.comparing(
            c -> c.getContratistaNombre() != null ? c.getContratistaNombre() : "",
            String.CASE_INSENSITIVE_ORDER
        ));

        long total = paginaContratos.getTotalElements();
        int totalPaginas = paginaContratos.getTotalPages();

        return new BusquedaAdminPageResponse(resultados, total, pagina, totalPaginas, tamano);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private List<ContratistaResultadoDto> buscarContratistas(String q, Pageable pageable) {
        List<Usuario> usuarios = usuarioRepository.buscarContratistas(q, pageable);
        return usuarios.stream()
            .map(u -> new ContratistaResultadoDto(u.getId(), u.getNombre(), u.getEmail(), u.getCargo()))
            .collect(Collectors.toList());
    }

    private List<ContratoResultadoDto> buscarContratosSimple(String q, Pageable pageable) {
        List<Contrato> contratos = contratoRepository.buscarContratos(q, pageable);
        return contratos.stream()
            .map(this::toContratoDto)
            .collect(Collectors.toList());
    }

    private List<InformeResultadoDto> buscarInformesSimple(String q, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable) {
        List<Informe> informes = informeRepository.buscarInformes(q, fechaInicio, fechaFin, pageable);
        return informes.stream()
            .map(this::toInformeDto)
            .collect(Collectors.toList());
    }

    private ContratoResultadoDto toContratoDto(Contrato c) {
        ContratoResultadoDto dto = new ContratoResultadoDto(
            c.getId(),
            c.getNumero(),
            c.getObjeto(),
            c.getEstado() != null ? c.getEstado().name() : null,
            c.getContratista() != null ? c.getContratista().getNombre() : null
        );
        dto.setContratistaId(c.getContratista() != null ? c.getContratista().getId() : null);
        return dto;
    }

    private InformeResultadoDto toInformeDto(Informe i) {
        String revisorNombre = i.getContrato() != null && i.getContrato().getRevisor() != null
            ? i.getContrato().getRevisor().getNombre() : null;
        return new InformeResultadoDto(
            i.getId(),
            i.getNumero(),
            i.getEstado() != null ? i.getEstado().name() : null,
            i.getFechaInicio() != null ? i.getFechaInicio().toString() : null,
            i.getFechaFin() != null ? i.getFechaFin().toString() : null,
            i.getContrato() != null ? i.getContrato().getId() : null,
            i.getContrato() != null ? i.getContrato().getNumero() : null,
            i.getContrato() != null && i.getContrato().getContratista() != null
                ? i.getContrato().getContratista().getNombre() : null,
            revisorNombre
        );
    }
}
