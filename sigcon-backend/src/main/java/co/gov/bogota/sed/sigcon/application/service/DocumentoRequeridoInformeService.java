package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoRequeridoDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.EmlPreviewDto;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoRequeridoInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoRequeridoInformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * I7: Gestiona los documentos requeridos por informe (PDF / EML).
 *
 * <p>Separado de DocumentoAdicionalInformeService (documentos adicionales libres).
 * La FACTURA es un requerido dinamico: se exige cuando el contratista del contrato
 * tiene responsableIva=true. No depende de parametrizacion manual en catalogo.</p>
 */
@Service
@Transactional
public class DocumentoRequeridoInformeService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoRequeridoInformeService.class);

    /** Clave logica reservada para la factura dinamica por IVA. */
    public static final String CLAVE_FACTURA = "FACTURA";

    private static final Set<String> EXTENSIONES_PERMITIDAS = new HashSet<>(Arrays.asList(".pdf", ".eml"));
    private static final Set<String> CONTENT_TYPES_PDF = new HashSet<>(Arrays.asList("application/pdf"));
    private static final Set<String> CONTENT_TYPES_EML = new HashSet<>(Arrays.asList(
        "message/rfc822", "application/octet-stream"
    ));

    private final DocumentoRequeridoInformeRepository repository;
    private final InformeService informeService;
    private final CurrentUserService currentUserService;
    private final DocumentStorageService storageService;

    public DocumentoRequeridoInformeService(
        DocumentoRequeridoInformeRepository repository,
        InformeService informeService,
        CurrentUserService currentUserService,
        DocumentStorageService storageService
    ) {
        this.repository = repository;
        this.informeService = informeService;
        this.currentUserService = currentUserService;
        this.storageService = storageService;
    }

    // -----------------------------------------------------------------------
    // Listar documentos requeridos del informe
    // -----------------------------------------------------------------------

    /**
     * Retorna la lista de documentos requeridos del informe.
     * Incluye FACTURA dinamica si el contratista es responsable de IVA.
     * Cualquier usuario con acceso al informe puede listar.
     */
    @Transactional(readOnly = true)
    public List<DocumentoRequeridoDto> listar(Long informeId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanViewInforme(usuario, informe);

        List<DocumentoRequeridoInforme> registros = repository.findByInformeIdAndActivoTrue(informeId);
        boolean requiereFactura = requiereFactura(informe);

        List<DocumentoRequeridoDto> resultado = new ArrayList<>();

        // Agregar FACTURA dinamica si aplica y no existe aun en BD
        if (requiereFactura) {
            boolean facturaEnBd = registros.stream()
                .anyMatch(r -> CLAVE_FACTURA.equals(r.getClaveLogica()));
            if (!facturaEnBd) {
                resultado.add(buildFacturaPendiente());
            }
        }

        for (DocumentoRequeridoInforme reg : registros) {
            resultado.add(toDto(reg, CLAVE_FACTURA.equals(reg.getClaveLogica()) && requiereFactura));
        }

        return resultado;
    }

    // -----------------------------------------------------------------------
    // Upload de archivo
    // -----------------------------------------------------------------------

    /**
     * Carga o reemplaza el archivo de un documento requerido.
     * Solo CONTRATISTA propietario, solo en BORRADOR o DEVUELTO.
     */
    public DocumentoRequeridoDto cargarArchivo(Long informeId, String claveLogica, MultipartFile file) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        assertPropietarioEditable(usuario, informe);

        // Validar que la clave sea FACTURA solo si el contratista es responsable IVA
        if (CLAVE_FACTURA.equals(claveLogica) && !requiereFactura(informe)) {
            throw new SigconBusinessException(
                ErrorCode.DOCUMENTO_REQUERIDO_NO_ENCONTRADO,
                "FACTURA no aplica para este contratista",
                HttpStatus.BAD_REQUEST
            );
        }

        String extension = resolverExtension(file);
        String contentType = resolverContentType(file, extension);

        String subdir = "docs-requeridos/" + informeId;
        String storagePath;
        try {
            storagePath = storageService.storeFile(subdir, file);
        } catch (IOException e) {
            log.error("Error almacenando documento requerido informeId={} clave={}", informeId, claveLogica, e);
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Error al almacenar el archivo",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        // Buscar registro existente o crear uno nuevo
        DocumentoRequeridoInforme registro = repository
            .findByInformeIdAndClaveLogicaAndActivoTrue(informeId, claveLogica)
            .orElseGet(() -> {
                DocumentoRequeridoInforme nuevo = new DocumentoRequeridoInforme();
                nuevo.setInforme(informe);
                nuevo.setClaveLogica(claveLogica);
                nuevo.setNombreDisplay(resolverNombreDisplay(claveLogica));
                nuevo.setActivo(true);
                return nuevo;
            });

        registro.setNombreArchivo(file.getOriginalFilename());
        registro.setContentType(contentType);
        registro.setExtension(extension);
        registro.setStoragePath(storagePath);
        registro.setTamanoBytes(file.getSize());

        DocumentoRequeridoInforme saved = repository.save(registro);
        return toDto(saved, CLAVE_FACTURA.equals(claveLogica) && requiereFactura(informe));
    }

    // -----------------------------------------------------------------------
    // Descarga de archivo
    // -----------------------------------------------------------------------

    /**
     * Retorna el InputStream del archivo almacenado.
     * Cualquier usuario con acceso al informe puede descargar.
     */
    @Transactional(readOnly = true)
    public InputStream descargarArchivo(Long informeId, Long documentoId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanViewInforme(usuario, informe);

        DocumentoRequeridoInforme registro = findRegistro(informeId, documentoId);
        assertCargado(registro);

        try {
            return storageService.loadFile(registro.getStoragePath());
        } catch (IOException e) {
            log.error("Error cargando archivo documentoId={}", documentoId, e);
            throw new SigconBusinessException(
                ErrorCode.PDF_NO_DISPONIBLE,
                "Archivo no disponible",
                HttpStatus.NOT_FOUND
            );
        }
    }

    /**
     * Retorna el DocumentoRequeridoInforme para que el controller pueda leer
     * nombre y content-type al servir la descarga.
     */
    @Transactional(readOnly = true)
    public DocumentoRequeridoInforme obtenerRegistroParaDescarga(Long informeId, Long documentoId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanViewInforme(usuario, informe);
        return findRegistro(informeId, documentoId);
    }

    // -----------------------------------------------------------------------
    // Preview EML
    // -----------------------------------------------------------------------

    /**
     * Retorna preview basico de un archivo .eml.
     * Cualquier usuario con acceso al informe puede previsualizar.
     */
    @Transactional(readOnly = true)
    public EmlPreviewDto previewEml(Long informeId, Long documentoId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanViewInforme(usuario, informe);

        DocumentoRequeridoInforme registro = findRegistro(informeId, documentoId);
        assertCargado(registro);

        if (!".eml".equalsIgnoreCase(registro.getExtension())) {
            throw new SigconBusinessException(
                ErrorCode.DOCUMENTO_REQUERIDO_FORMATO_INVALIDO,
                "El documento no es un archivo EML",
                HttpStatus.BAD_REQUEST
            );
        }

        try (InputStream is = storageService.loadFile(registro.getStoragePath())) {
            return parsearEml(is);
        } catch (IOException e) {
            log.error("Error leyendo EML documentoId={}", documentoId, e);
            throw new SigconBusinessException(
                ErrorCode.PDF_NO_DISPONIBLE,
                "Archivo EML no disponible",
                HttpStatus.NOT_FOUND
            );
        }
    }

    // -----------------------------------------------------------------------
    // Eliminar archivo
    // -----------------------------------------------------------------------

    /**
     * Elimina (soft-delete) el registro de un documento requerido.
     * Solo CONTRATISTA propietario, solo en BORRADOR o DEVUELTO.
     */
    public void eliminarArchivo(Long informeId, Long documentoId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        assertPropietarioEditable(usuario, informe);

        DocumentoRequeridoInforme registro = findRegistro(informeId, documentoId);
        registro.setActivo(false);
        repository.save(registro);
    }

    // -----------------------------------------------------------------------
    // Validacion para envio (usado por InformeEstadoService en T5)
    // -----------------------------------------------------------------------

    /**
     * Verifica que todos los documentos requeridos esten cargados.
     * Incluye FACTURA si el contratista es responsable de IVA.
     * Lanza SigconBusinessException si falta alguno.
     */
    public void assertDocumentosRequeridosCompletos(Informe informe) {
        if (requiereFactura(informe)) {
            boolean facturaCargada = repository.existsByInformeIdAndClaveLogicaAndStoragePathIsNotNullAndActivoTrue(
                informe.getId(), CLAVE_FACTURA
            );
            if (!facturaCargada) {
                throw new SigconBusinessException(
                    ErrorCode.DOCUMENTO_REQUERIDO_FALTANTE,
                    "El contratista es responsable de IVA: debe cargar la FACTURA antes de enviar el informe",
                    HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------------

    private boolean requiereFactura(Informe informe) {
        return Boolean.TRUE.equals(
            informe.getContrato().getContratista().getResponsableIva()
        );
    }

    private DocumentoRequeridoInforme findRegistro(Long informeId, Long documentoId) {
        DocumentoRequeridoInforme registro = repository.findByIdAndActivoTrue(documentoId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.DOCUMENTO_REQUERIDO_NO_ENCONTRADO,
                "Documento requerido no encontrado",
                HttpStatus.NOT_FOUND
            ));
        if (!registro.getInforme().getId().equals(informeId)) {
            throw new SigconBusinessException(
                ErrorCode.ACCESO_DENEGADO,
                "El documento no pertenece al informe indicado",
                HttpStatus.FORBIDDEN
            );
        }
        return registro;
    }

    private void assertPropietarioEditable(Usuario usuario, Informe informe) {
        if (usuario.getRol() != RolUsuario.CONTRATISTA) {
            throw accessDenied();
        }
        if (!informe.getContrato().getContratista().getId().equals(usuario.getId())) {
            throw accessDenied();
        }
        EstadoInforme estado = informe.getEstado();
        if (estado != EstadoInforme.BORRADOR && estado != EstadoInforme.DEVUELTO) {
            throw new SigconBusinessException(
                ErrorCode.DOCUMENTO_REQUERIDO_NO_EDITABLE,
                "Los documentos requeridos solo se pueden modificar en estado BORRADOR o DEVUELTO",
                HttpStatus.CONFLICT
            );
        }
    }

    private void assertCargado(DocumentoRequeridoInforme registro) {
        if (!registro.isCargado()) {
            throw new SigconBusinessException(
                ErrorCode.PDF_NO_DISPONIBLE,
                "El documento requerido aun no tiene archivo cargado",
                HttpStatus.NOT_FOUND
            );
        }
    }

    private String resolverExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) {
                String ext = originalName.substring(dot).toLowerCase();
                if (EXTENSIONES_PERMITIDAS.contains(ext)) {
                    return ext;
                }
            }
        }
        throw new SigconBusinessException(
            ErrorCode.DOCUMENTO_REQUERIDO_FORMATO_INVALIDO,
            "Solo se permiten archivos PDF y EML",
            HttpStatus.BAD_REQUEST
        );
    }

    private String resolverContentType(MultipartFile file, String extension) {
        String ct = file.getContentType();
        if (".pdf".equals(extension)) {
            if (ct != null && CONTENT_TYPES_PDF.contains(ct)) {
                return ct;
            }
            // Aceptar si la extension es .pdf aunque el content-type sea generico
            return "application/pdf";
        }
        // .eml
        if (ct != null && CONTENT_TYPES_EML.contains(ct)) {
            return ct;
        }
        return "message/rfc822";
    }

    private String resolverNombreDisplay(String claveLogica) {
        if (CLAVE_FACTURA.equals(claveLogica)) {
            return "Factura (IVA)";
        }
        return claveLogica;
    }

    private DocumentoRequeridoDto buildFacturaPendiente() {
        DocumentoRequeridoDto dto = new DocumentoRequeridoDto();
        dto.setClaveLogica(CLAVE_FACTURA);
        dto.setNombreDisplay("Factura (IVA)");
        dto.setCargado(false);
        dto.setPorIva(true);
        return dto;
    }

    private DocumentoRequeridoDto toDto(DocumentoRequeridoInforme reg, boolean porIva) {
        DocumentoRequeridoDto dto = new DocumentoRequeridoDto();
        dto.setId(reg.getId());
        dto.setClaveLogica(reg.getClaveLogica());
        dto.setNombreDisplay(reg.getNombreDisplay());
        dto.setCargado(reg.isCargado());
        dto.setNombreArchivo(reg.getNombreArchivo());
        dto.setContentType(reg.getContentType());
        dto.setExtension(reg.getExtension());
        dto.setTamanoBytes(reg.getTamanoBytes());
        dto.setPorIva(porIva);
        return dto;
    }

    // -----------------------------------------------------------------------
    // Parser EML con javax.mail (disponible en Spring Boot 2.7.x via spring-boot-starter-mail)
    // -----------------------------------------------------------------------

    private EmlPreviewDto parsearEml(InputStream is) {
        EmlPreviewDto preview = new EmlPreviewDto();
        try {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage msg = new MimeMessage(session, is);

            preview.setAsunto(msg.getSubject());
            preview.setFecha(msg.getSentDate() != null ? msg.getSentDate().toString() : null);

            Address[] from = msg.getFrom();
            if (from != null && from.length > 0) {
                preview.setRemitente(from[0].toString());
            }

            Address[] to = msg.getRecipients(Message.RecipientType.TO);
            if (to != null && to.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (Address a : to) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(a.toString());
                }
                preview.setDestinatarios(sb.toString());
            }

            String cuerpo = extraerCuerpoTexto(msg);
            preview.setCuerpoTexto(cuerpo);
            preview.setPreviewParcial(cuerpo == null || cuerpo.isEmpty());

        } catch (MessagingException e) {
            log.warn("No se pudo parsear el EML completamente: {}", e.getMessage());
            preview.setPreviewParcial(true);
        }
        return preview;
    }

    private String extraerCuerpoTexto(Part part) {
        try {
            if (part.isMimeType("text/plain")) {
                Object content = part.getContent();
                return content != null ? content.toString() : null;
            }
            if (part.isMimeType("text/html")) {
                Object content = part.getContent();
                if (content != null) {
                    // Retornar HTML como texto plano basico (sin parsear)
                    return content.toString().replaceAll("<[^>]+>", " ").trim();
                }
            }
            if (part.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) part.getContent();
                for (int i = 0; i < mp.getCount(); i++) {
                    String texto = extraerCuerpoTexto(mp.getBodyPart(i));
                    if (texto != null && !texto.isEmpty()) {
                        return texto;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer cuerpo de parte EML: {}", e.getMessage());
        }
        return null;
    }

    private static SigconBusinessException accessDenied() {
        return new SigconBusinessException(ErrorCode.ACCESO_DENEGADO, "Acceso denegado", HttpStatus.FORBIDDEN);
    }
}
