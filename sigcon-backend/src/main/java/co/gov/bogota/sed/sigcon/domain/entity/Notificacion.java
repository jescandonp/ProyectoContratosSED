package co.gov.bogota.sed.sigcon.domain.entity;

import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
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
import java.time.LocalDateTime;

/**
 * Notificacion in-app generada por eventos del flujo de informes (I3).
 * Tabla: SGCN_NOTIFICACIONES.
 */
@Entity
@Table(name = "SGCN_NOTIFICACIONES")
@EntityListeners(AuditingEntityListener.class)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificaciones_seq")
    @SequenceGenerator(name = "notificaciones_seq", sequenceName = "SGCN_NOTIFICACIONES_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    /** Destinatario de la notificacion. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @Column(name = "TITULO", nullable = false, length = 200)
    private String titulo;

    @Column(name = "DESCRIPCION", nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_EVENTO", nullable = false, length = 50)
    private TipoEvento tipoEvento;

    /**
     * Informe asociado (nullable — notificaciones futuras pueden no tener informe).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_INFORME")
    private Informe informe;

    @Column(name = "LEIDA", nullable = false)
    private boolean leida = false;

    /**
     * Timestamp de la notificacion; lo aplica Oracle con DEFAULT SYSTIMESTAMP.
     * insertable=false, updatable=false para no interferir con el DEFAULT.
     */
    @Column(name = "FECHA", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fecha;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 200, updatable = false)
    private String createdBy;

    @Column(name = "UPDATED_AT", insertable = false)
    private LocalDateTime updatedAt;

    // ---- Getters / Setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Informe getInforme() {
        return informe;
    }

    public void setInforme(Informe informe) {
        this.informe = informe;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
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
