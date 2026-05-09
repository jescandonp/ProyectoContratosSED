package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiDto;
import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiRequest;
import co.gov.bogota.sed.sigcon.application.mapper.AporteSgssiMapper;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.repository.AporteSgssiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AporteSgssiService {

    private final AporteSgssiRepository repository;
    private final InformeService informeService;
    private final CurrentUserService currentUserService;
    private final AporteSgssiMapper mapper;

    public AporteSgssiService(
        AporteSgssiRepository repository,
        InformeService informeService,
        CurrentUserService currentUserService,
        AporteSgssiMapper mapper
    ) {
        this.repository = repository;
        this.informeService = informeService;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AporteSgssiDto> listar(Long informeId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanViewInforme(usuario, informe);
        return repository.findByInformeIdAndActivoTrue(informeId).stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }

    public List<AporteSgssiDto> guardarTodos(Long informeId, List<AporteSgssiRequest> requests) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanEditInforme(usuario, informe);

        List<AporteSgssi> existing = repository.findByInformeIdAndActivoTrue(informeId);
        for (AporteSgssi a : existing) {
            a.setActivo(false);
        }
        repository.saveAll(existing);

        List<AporteSgssi> nuevos = new ArrayList<>();
        for (AporteSgssiRequest req : requests) {
            AporteSgssi aporte = new AporteSgssi();
            aporte.setInforme(informe);
            aporte.setItem(req.getItem());
            aporte.setFechaPago(req.getFechaPago());
            aporte.setValorAportado(req.getValorAportado());
            aporte.setEntidad(req.getEntidad());
            aporte.setActivo(true);
            nuevos.add(aporte);
        }
        return repository.saveAll(nuevos).stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }
}
