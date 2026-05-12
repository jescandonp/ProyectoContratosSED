package co.gov.bogota.sed.sigcon.application.dto.busqueda;

import java.util.List;

/** I7: Respuesta agrupada de la búsqueda administrativa global. */
public class BusquedaAdminResponse {
    private List<ContratistaResultadoDto> contratistas;
    private List<ContratoResultadoDto> contratos;
    private List<InformeResultadoDto> informes;

    public BusquedaAdminResponse(
        List<ContratistaResultadoDto> contratistas,
        List<ContratoResultadoDto> contratos,
        List<InformeResultadoDto> informes
    ) {
        this.contratistas = contratistas;
        this.contratos = contratos;
        this.informes = informes;
    }

    public List<ContratistaResultadoDto> getContratistas() { return contratistas; }
    public void setContratistas(List<ContratistaResultadoDto> contratistas) { this.contratistas = contratistas; }
    public List<ContratoResultadoDto> getContratos() { return contratos; }
    public void setContratos(List<ContratoResultadoDto> contratos) { this.contratos = contratos; }
    public List<InformeResultadoDto> getInformes() { return informes; }
    public void setInformes(List<InformeResultadoDto> informes) { this.informes = informes; }
}
