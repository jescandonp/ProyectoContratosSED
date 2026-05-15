package co.gov.bogota.sed.sigcon.application.dto.busqueda;

import java.util.ArrayList;
import java.util.List;

/** I7/T11: Resultado de búsqueda global — contrato con informes anidados. */
public class ContratoResultadoDto {
    private Long id;
    private String numero;
    private String objeto;
    private String estado;
    private String contratistaNombre;
    private Long contratistaId;
    /** T11: informes del contrato que cumplen los filtros de búsqueda. */
    private List<InformeResultadoDto> informes = new ArrayList<>();

    public ContratoResultadoDto() {}

    public ContratoResultadoDto(Long id, String numero, String objeto, String estado, String contratistaNombre) {
        this.id = id;
        this.numero = numero;
        this.objeto = objeto;
        this.estado = estado;
        this.contratistaNombre = contratistaNombre;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getObjeto() { return objeto; }
    public void setObjeto(String objeto) { this.objeto = objeto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getContratistaNombre() { return contratistaNombre; }
    public void setContratistaNombre(String contratistaNombre) { this.contratistaNombre = contratistaNombre; }
    public Long getContratistaId() { return contratistaId; }
    public void setContratistaId(Long contratistaId) { this.contratistaId = contratistaId; }
    public List<InformeResultadoDto> getInformes() { return informes; }
    public void setInformes(List<InformeResultadoDto> informes) { this.informes = informes; }
}
