package co.gov.bogota.sed.sigcon.application.dto.informe;

import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;

import java.time.LocalDateTime;

public class ObservacionDto {
    private Long id;
    private String texto;
    private RolObservacion autorRol;
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public RolObservacion getAutorRol() { return autorRol; }
    public void setAutorRol(RolObservacion autorRol) { this.autorRol = autorRol; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
