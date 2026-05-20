package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.domain.entity.SgcnParametro;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SgcnParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParametroService {

    public static final String VB_ACTIVO = "VB_ACTIVO";

    private static final String VALOR_SI = "S";
    private static final String VALOR_NO = "N";
    private static final String DESCRIPCION_VB =
        "Visto Bueno Administrativo activo en el flujo de informes";

    private final SgcnParametroRepository parametroRepository;
    private final InformeRepository informeRepository;

    public ParametroService(
        SgcnParametroRepository parametroRepository,
        InformeRepository informeRepository
    ) {
        this.parametroRepository = parametroRepository;
        this.informeRepository = informeRepository;
    }

    @Transactional(readOnly = true)
    public boolean isVbActivo() {
        return parametroRepository.findById(VB_ACTIVO)
            .map(SgcnParametro::getValor)
            .map(VALOR_SI::equalsIgnoreCase)
            .orElse(false);
    }

    public void setVbActivo(boolean activo) {
        SgcnParametro parametro = parametroRepository.findById(VB_ACTIVO)
            .orElseGet(this::nuevoParametroVb);

        parametro.setValor(activo ? VALOR_SI : VALOR_NO);
        parametro.setDescripcion(DESCRIPCION_VB);
        parametroRepository.save(parametro);

        if (!activo) {
            informeRepository.migrarEnVistoBuenoAEnRevision();
        }
    }

    private SgcnParametro nuevoParametroVb() {
        SgcnParametro parametro = new SgcnParametro();
        parametro.setClave(VB_ACTIVO);
        parametro.setDescripcion(DESCRIPCION_VB);
        return parametro;
    }
}
