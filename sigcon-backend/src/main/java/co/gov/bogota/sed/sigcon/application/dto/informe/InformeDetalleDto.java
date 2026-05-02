package co.gov.bogota.sed.sigcon.application.dto.informe;

import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioDto;

import java.util.ArrayList;
import java.util.List;

public class InformeDetalleDto extends InformeResumenDto {
    private UsuarioDto contratista;
    private UsuarioDto revisor;
    private UsuarioDto supervisor;
    private List<ActividadInformeDto> actividades = new ArrayList<>();
    private List<DocumentoAdicionalDto> documentosAdicionales = new ArrayList<>();
    private List<ObservacionDto> observaciones = new ArrayList<>();

    public UsuarioDto getContratista() { return contratista; }
    public void setContratista(UsuarioDto contratista) { this.contratista = contratista; }
    public UsuarioDto getRevisor() { return revisor; }
    public void setRevisor(UsuarioDto revisor) { this.revisor = revisor; }
    public UsuarioDto getSupervisor() { return supervisor; }
    public void setSupervisor(UsuarioDto supervisor) { this.supervisor = supervisor; }
    public List<ActividadInformeDto> getActividades() { return actividades; }
    public void setActividades(List<ActividadInformeDto> actividades) { this.actividades = actividades; }
    public List<DocumentoAdicionalDto> getDocumentosAdicionales() { return documentosAdicionales; }
    public void setDocumentosAdicionales(List<DocumentoAdicionalDto> documentosAdicionales) { this.documentosAdicionales = documentosAdicionales; }
    public List<ObservacionDto> getObservaciones() { return observaciones; }
    public void setObservaciones(List<ObservacionDto> observaciones) { this.observaciones = observaciones; }
}
