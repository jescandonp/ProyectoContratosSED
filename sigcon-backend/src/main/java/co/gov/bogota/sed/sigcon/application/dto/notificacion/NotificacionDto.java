package co.gov.bogota.sed.sigcon.application.dto.notificacion;

import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import java.time.LocalDateTime;

public class NotificacionDto {

    private Long id;
    private String titulo;
    private String descripcion;
    private TipoEvento tipoEvento;
    private Long idInforme;
    private boolean leida;
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public TipoEvento getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEvento tipoEvento) { this.tipoEvento = tipoEvento; }

    public Long getIdInforme() { return idInforme; }
    public void setIdInforme(Long idInforme) { this.idInforme = idInforme; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
