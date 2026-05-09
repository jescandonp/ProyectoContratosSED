package co.gov.bogota.sed.sigcon.application.dto.sgssi;

import co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AporteSgssiDto {

    private Long id;
    private ItemSgssi item;
    private LocalDate fechaPago;
    private BigDecimal valorAportado;
    private String entidad;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ItemSgssi getItem() { return item; }
    public void setItem(ItemSgssi item) { this.item = item; }
    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }
    public BigDecimal getValorAportado() { return valorAportado; }
    public void setValorAportado(BigDecimal valorAportado) { this.valorAportado = valorAportado; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
}
