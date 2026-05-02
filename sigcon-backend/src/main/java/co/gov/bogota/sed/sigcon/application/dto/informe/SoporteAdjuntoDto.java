package co.gov.bogota.sed.sigcon.application.dto.informe;

import co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte;

public class SoporteAdjuntoDto {
    private Long id;
    private TipoSoporte tipo;
    private String nombre;
    private String referencia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoSoporte getTipo() { return tipo; }
    public void setTipo(TipoSoporte tipo) { this.tipo = tipo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
