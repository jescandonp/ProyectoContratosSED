package co.gov.bogota.sed.sigcon.domain.entity;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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
 * I7: Archivo requerido por informe (PDF o EML).
 * Separado de DocumentoAdicional (documentos adicionales libres con referencia textual).
 * CLAVE_LOGICA identifica el tipo; 'FACTURA' es dinamico para contratistas responsables de IVA.
 */
@Entity
@Table(name = "SGCN_DOCS_REQUERIDOS")
@EntityListeners(AuditingEntityListener.class)
public class DocumentoRequeridoInforme {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "docs_requeridos_seq")
    @SequenceGenerator(name = "docs_requeridos_seq", sequenceName = "SGCN_DOCS_REQUERIDOS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_INFORME", nullable = false)
    private Informe informe;

    /** Clave logica del tipo de documento (ej. 'FACTURA', 'POLIZA', etc.). */
    @Column(name = "CLAVE_LOGICA", nullable = false, length = 100)
    private String claveLogica;

    /** Nombre legible para mostrar en UI. */
    @Column(name = "NOMBRE_DISPLAY", nullable = false, length = 200)
    private String nombreDisplay;

    /** Nombre original del archivo cargado. Null si aun no se ha cargado. */
    @Column(name = "NOMBRE_ARCHIVO", length = 500)
    private String nombreArchivo;

    /** Content-type del archivo (application/pdf, message/rfc822, etc.). */
    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType;

    /** Extension del archivo (.pdf, .eml). */
    @Column(name = "EXTENSION", length = 10)
    private String extension;

    /** Ruta relativa en DocumentStorageService. Null si aun no se ha cargado. */
    @Column(name = "STORAGE_PATH", length = 1000)
    private String storagePath;

    /** Tamano en bytes del archivo cargado. */
    @Column(name = "TAMANO_BYTES")
    private Long tamanoBytes;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 200, updatable = false)
    private String createdBy;

    @Column(name = "UPDATED_AT", insertable = false)
    private LocalDateTime updatedAt;

    // ---- Getters / Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Informe getInforme() { return informe; }
    public void setInforme(Informe informe) { this.informe = informe; }

    public String getClaveLogica() { return claveLogica; }
    public void setClaveLogica(String claveLogica) { this.claveLogica = claveLogica; }

    public String getNombreDisplay() { return nombreDisplay; }
    public void setNombreDisplay(String nombreDisplay) { this.nombreDisplay = nombreDisplay; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Retorna true si el archivo ya fue cargado (storagePath no es null). */
    public boolean isCargado() {
        return storagePath != null && !storagePath.trim().isEmpty();
    }
}
