package co.gov.bogota.sed.sigcon.application.dto.contrato;

import co.gov.bogota.sed.sigcon.application.dto.catalogo.DocumentoCatalogoDto;
import co.gov.bogota.sed.sigcon.application.dto.obligacion.ObligacionDto;
import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioDto;

import java.util.ArrayList;
import java.util.List;

public class ContratoDetalleDto extends ContratoResumenDto {
    private UsuarioDto contratista;
    private UsuarioDto revisor;
    private UsuarioDto supervisor;
    private List<ObligacionDto> obligaciones = new ArrayList<>();
    private List<DocumentoCatalogoDto> docsAplicables = new ArrayList<>();

    public UsuarioDto getContratista() { return contratista; }
    public void setContratista(UsuarioDto contratista) { this.contratista = contratista; }
    public UsuarioDto getRevisor() { return revisor; }
    public void setRevisor(UsuarioDto revisor) { this.revisor = revisor; }
    public UsuarioDto getSupervisor() { return supervisor; }
    public void setSupervisor(UsuarioDto supervisor) { this.supervisor = supervisor; }
    public List<ObligacionDto> getObligaciones() { return obligaciones; }
    public void setObligaciones(List<ObligacionDto> obligaciones) { this.obligaciones = obligaciones; }
    public List<DocumentoCatalogoDto> getDocsAplicables() { return docsAplicables; }
    public void setDocsAplicables(List<DocumentoCatalogoDto> docsAplicables) { this.docsAplicables = docsAplicables; }
}
