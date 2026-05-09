package co.gov.bogota.sed.sigcon.domain.entity;

import co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "SGCN_APORTES_SGSSI")
@EntityListeners(AuditingEntityListener.class)
public class AporteSgssi {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aportes_sgssi_seq")
    @SequenceGenerator(name = "aportes_sgssi_seq", sequenceName = "SGCN_APORTES_SGSSI_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_INFORME", nullable = false)
    private Informe informe;

    @Enumerated(EnumType.STRING)
    @Column(name = "ITEM", nullable = false, length = 20)
    private ItemSgssi item;

    @Column(name = "FECHA_PAGO", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "VALOR_APORTADO", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAportado;

    @Column(name = "ENTIDAD", nullable = false, length = 200)
    private String entidad;

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo = 1;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 200, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "UPDATED_AT", insertable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Informe getInforme() {
        return informe;
    }

    public void setInforme(Informe informe) {
        this.informe = informe;
    }

    public ItemSgssi getItem() {
        return item;
    }

    public void setItem(ItemSgssi item) {
        this.item = item;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public BigDecimal getValorAportado() {
        return valorAportado;
    }

    public void setValorAportado(BigDecimal valorAportado) {
        this.valorAportado = valorAportado;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
