package co.gov.bogota.sed.sigcon.application.dto.busqueda;

import java.util.List;

/**
 * T11: Respuesta paginada de la búsqueda administrativa global.
 * Retorna contratos con sus informes anidados que cumplen los filtros.
 */
public class BusquedaAdminPageResponse {

    private List<ContratoResultadoDto> contratos;
    private long totalElementos;
    private int paginaActual;
    private int totalPaginas;
    private int tamano;

    public BusquedaAdminPageResponse() {}

    public BusquedaAdminPageResponse(
        List<ContratoResultadoDto> contratos,
        long totalElementos,
        int paginaActual,
        int totalPaginas,
        int tamano
    ) {
        this.contratos = contratos;
        this.totalElementos = totalElementos;
        this.paginaActual = paginaActual;
        this.totalPaginas = totalPaginas;
        this.tamano = tamano;
    }

    public List<ContratoResultadoDto> getContratos() { return contratos; }
    public void setContratos(List<ContratoResultadoDto> contratos) { this.contratos = contratos; }

    public long getTotalElementos() { return totalElementos; }
    public void setTotalElementos(long totalElementos) { this.totalElementos = totalElementos; }

    public int getPaginaActual() { return paginaActual; }
    public void setPaginaActual(int paginaActual) { this.paginaActual = paginaActual; }

    public int getTotalPaginas() { return totalPaginas; }
    public void setTotalPaginas(int totalPaginas) { this.totalPaginas = totalPaginas; }

    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
}
