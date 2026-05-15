package co.gov.bogota.sed.sigcon.application.dto.busqueda;

import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;

import java.time.LocalDate;

/**
 * T11: Filtros combinados para la búsqueda administrativa global.
 * Todos los campos son opcionales; si se omiten no se aplica ese filtro.
 */
public class BusquedaAdminFiltros {

    /** Texto libre opcional (nombre, número, email, objeto, estado). */
    private String q;

    /** Filtro por estado del contrato. */
    private EstadoContrato estadoContrato;

    /** Inicio del rango de periodo del informe (fechaInicio del informe). */
    private LocalDate fechaInicio;

    /** Fin del rango de periodo del informe (fechaFin del informe). */
    private LocalDate fechaFin;

    /** Filtro por ID del contratista. */
    private Long contratistaId;

    /** Filtro por ID del revisor. */
    private Long revisorId;

    /** Filtro por estado del informe. */
    private EstadoInforme estadoInforme;

    /** Número de página (0-based). */
    private int pagina = 0;

    /** Tamaño de página. Default 20 según spec T11. */
    private int tamano = 20;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public EstadoContrato getEstadoContrato() { return estadoContrato; }
    public void setEstadoContrato(EstadoContrato estadoContrato) { this.estadoContrato = estadoContrato; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Long getContratistaId() { return contratistaId; }
    public void setContratistaId(Long contratistaId) { this.contratistaId = contratistaId; }

    public Long getRevisorId() { return revisorId; }
    public void setRevisorId(Long revisorId) { this.revisorId = revisorId; }

    public EstadoInforme getEstadoInforme() { return estadoInforme; }
    public void setEstadoInforme(EstadoInforme estadoInforme) { this.estadoInforme = estadoInforme; }

    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }

    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
}
