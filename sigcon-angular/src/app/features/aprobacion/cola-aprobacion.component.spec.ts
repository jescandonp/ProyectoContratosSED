import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { InformeDetalle, InformeResumen } from '../../core/models/informe.model';
import { Page } from '../../core/models/page.model';
import { InformeService } from '../../core/services/informe.service';
import { ObservacionService } from '../../core/services/observacion.service';
import { ColaAprobacionComponent } from './cola-aprobacion.component';

describe('ColaAprobacionComponent', () => {
  let fixture: ComponentFixture<ColaAprobacionComponent>;
  let component: ColaAprobacionComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let observacionService: jasmine.SpyObj<ObservacionService>;
  let router: jasmine.SpyObj<Router>;

  const pageConInformes = samplePage([sampleInformeEnRevision(1), sampleInformeEnRevision(2)]);
  const pageVacia = samplePage([]);

  beforeEach(async () => {
    informeService = jasmine.createSpyObj<InformeService>('InformeService', ['listarInformes']);
    observacionService = jasmine.createSpyObj<ObservacionService>('ObservacionService', [
      'aprobarInforme',
      'devolverInforme'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    informeService.listarInformes.and.returnValue(of(pageConInformes));
    observacionService.aprobarInforme.and.returnValue(of(sampleInformeAprobado(1)));
    observacionService.devolverInforme.and.returnValue(of(sampleInformeDevuelto(1)));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [ColaAprobacionComponent],
      providers: [
        { provide: InformeService, useValue: informeService },
        { provide: ObservacionService, useValue: observacionService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ColaAprobacionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the paginated list of EN_REVISION informes on init', () => {
    expect(informeService.listarInformes).toHaveBeenCalledWith({ page: 0, size: 10 });
    expect(component.page()?.content.length).toBe(2);
  });

  it('shows empty state when there are no informes', () => {
    informeService.listarInformes.and.returnValue(of(pageVacia));
    fixture = TestBed.createComponent(ColaAprobacionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.page()?.content.length).toBe(0);
  });

  it('navigates to informe detail when "Ver detalle" is clicked', () => {
    const informe = pageConInformes.content[0];
    component.verDetalle(informe);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', informe.id]);
  });

  it('calls aprobarInforme and reloads the list', () => {
    const informe = pageConInformes.content[0];
    component.aprobar(informe);

    expect(observacionService.aprobarInforme).toHaveBeenCalledWith(informe.id);
    expect(informeService.listarInformes).toHaveBeenCalledTimes(2);
  });

  it('blocks devolucion when observacion is empty', () => {
    const informe = pageConInformes.content[0];
    component.abrirDialogoDevolucion(informe);
    component.textoObservacion.set('');

    component.confirmarDevolucion();

    expect(component.errorObservacion()).toBeTruthy();
    expect(observacionService.devolverInforme).not.toHaveBeenCalled();
  });

  it('calls devolverInforme with observacion and reloads the list', () => {
    const informe = pageConInformes.content[0];
    component.abrirDialogoDevolucion(informe);
    component.textoObservacion.set('Falta firma del supervisor');

    component.confirmarDevolucion();

    expect(observacionService.devolverInforme).toHaveBeenCalledWith(informe.id, { texto: 'Falta firma del supervisor' });
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

function sampleInformeEnRevision(id: number): InformeResumen {
  return {
    id,
    numero: id,
    contratoId: 10,
    contratoNumero: `OPS-2026-00${id}`,
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'EN_REVISION',
    fechaUltimoEnvio: '2026-05-10T10:00:00',
    fechaAprobacion: null
  };
}

function sampleInformeAprobado(id: number): InformeDetalle {
  return {
    id,
    numero: id,
    contratoId: 10,
    contratoNumero: `OPS-2026-00${id}`,
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'APROBADO',
    fechaUltimoEnvio: '2026-05-10T10:00:00',
    fechaAprobacion: '2026-05-15T14:00:00',
    contratista: null,
    revisor: null,
    supervisor: null,
    actividades: [],
    documentosAdicionales: [],
    observaciones: []
  };
}

function sampleInformeDevuelto(id: number): InformeDetalle {
  return {
    id,
    numero: id,
    contratoId: 10,
    contratoNumero: `OPS-2026-00${id}`,
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'DEVUELTO',
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
