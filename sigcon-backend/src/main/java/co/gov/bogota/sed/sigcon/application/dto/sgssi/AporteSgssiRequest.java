package co.gov.bogota.sed.sigcon.application.dto.sgssi;

import co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AporteSgssiRequest {

    @NotNull
    private ItemSgssi item;

    @NotNull
    private LocalDate fechaPago;

    @NotNull
    private BigDecimal valorAportado;

    @NotNull
    @Size(max = 200)
    private String entidad;

    public ItemSgssi getItem() { return item; }
    public void setItem(ItemSgssi item) { this.item = item; }
    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }
    public BigDecimal getValorAportado() { return valorAportado; }
    public void setValorAportado(BigDecimal valorAportado) { this.valorAportado = valorAportado; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
}
