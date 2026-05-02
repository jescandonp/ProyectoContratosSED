import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { InformeDetalle, InformeResumen } from '../../core/models/informe.model';
import { Page } from '../../core/models/page.model';
import { InformeService } from '../../core/services/informe.service';
import { ObservacionService } from '../../core/services/observacion.service';
import { ColaRevisionComponent } from './cola-revision.component';

describe('ColaRevisionComponent', () => {
  let fixture: ComponentFixture<ColaRevisionComponent>;
  let component: ColaRevisionComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let observacionService: jasmine.SpyObj<ObservacionService>;
  let router: jasmine.SpyObj<Router>;

  const pageConInformes = samplePage([sampleInformeEnviado(1), sampleInformeEnviado(2)]);
  const pageVacia = samplePage([]);

  beforeEach(async () => {
    informeService = jasmine.createSpyObj<InformeService>('InformeService', ['listarInformes']);
    observacionService = jasmine.createSpyObj<ObservacionService>('ObservacionService', [
      'aprobarRevision',
      'devolverRevision'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    informeService.listarInformes.and.returnValue(of(pageConInformes));
    observacionService.aprobarRevision.and.returnValue(of(sampleInformeDetalle(1)));
    observacionService.devolverRevision.and.returnValue(of(sampleInformeDetalle(1)));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [ColaRevisionComponent],
      providers: [
        { provide: InformeService, useValue: informeService },
        { provide: ObservacionService, useValue: observacionService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ColaRevisionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the paginated list of ENVIADO informes on init', () => {
    expect(informeService.listarInformes).toHaveBeenCalledWith({ page: 0, size: 10 });
    expect(component.page()?.content.length).toBe(2);
  });

  it('shows empty state when there are no informes', async () => {
    informeService.listarInformes.and.returnValue(of(pageVacia));
    fixture = TestBed.createComponent(ColaRevisionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.page()?.content.length).toBe(0);
  });

  it('navigates to informe detail when "Ver detalle" is clicked', () => {
    const informe = pageConInformes.content[0];
    component.verDetalle(informe);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', informe.id]);
  });

  it('calls aprobarRevision and reloads the list', () => {
    const informe = pageConInformes.content[0];
    component.aprobarRevision(informe);

    expect(observacionService.aprobarRevision).toHaveBeenCalledWith(informe.id);
    expect(informeService.listarInformes).toHaveBeenCalledTimes(2);
  });

  it('blocks devolucion when observacion is empty', () => {
    const informe = pageConInformes.content[0];
    component.abrirDialogoDevolucion(informe);
    component.textoObservacion.set('');

    component.confirmarDevolucion();

    expect(component.errorObservacion()).toBeTruthy();
    expect(observacionService.devolverRevision).not.toHaveBeenCalled();
  });

  it('calls devolverRevision with observacion and reloads the list', () => {
    const informe = pageConInformes.content[0];
    component.abrirDialogoDevolucion(informe);
    component.textoObservacion.set('Falta soporte en obligación 1');

    component.confirmarDevolucion();

    expect(observacionService.devolverRevision).toHaveBeenCalledWith(informe.id, { texto: 'Falta soporte en obligación 1' });
    expect(informeService.listarInformes).toHaveBeenCalledTimes(2);
  });

  it('closes the dialog after successful devolucion', () => {
    const informe = pageConInformes.content[0];
    component.abrirDialogoDevolucion(informe);
    component.textoObservacion.set('Observación de prueba');

    component.confirmarDevolucion();

    expect(component.dialogoDevolucion()).toBeNull();
  });
});

function sampleInformeEnviado(id: number): InformeResumen {
  return {
    id,
    numero: id,
    contratoId: 10,
    contratoNumero: `OPS-2026-00${id}`,
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'ENVIADO',
    fechaUltimoEnvio: '2026-05-10T10:00:00',
    fechaAprobacion: null
  };
}

function sampleInformeDetalle(id: number): InformeDetalle {
  return {
    id,
    numero: id,
    contratoId: 10,
    contratoNumero: `OPS-2026-00${id}`,
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'EN_REVISION',
    fechaUltimoEnvio: '2026-05-10T10:00:00',
    fechaAprobacion: null,
    contratista: null,
    revisor: null,
    supervisor: null,
    actividades: [],
    documentosAdicionales: [],
    observaciones: []
  };
}

function samplePage(content: InformeResumen[]): Page<InformeResumen> {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 10,
    number: 0,
    first: true,
    last: true
  };
}
