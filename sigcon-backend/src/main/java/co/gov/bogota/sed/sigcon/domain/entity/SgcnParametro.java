package co.gov.bogota.sed.sigcon.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "SGCN_PARAMETROS")
public class SgcnParametro {

    @Id
    @Column(name = "CLAVE", nullable = false, length = 50)
    private String clave;

    @Column(name = "VALOR", nullable = false, length = 200)
    private String valor;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
