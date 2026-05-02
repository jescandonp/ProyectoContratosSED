import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ContratoDetalle } from '../../../core/models/contrato.model';
import { InformeResumen } from '../../../core/models/informe.model';
import { Page } from '../../../core/models/page.model';
import { ContratoService } from '../../../core/services/contrato.service';
import { InformeService } from '../../../core/services/informe.service';
import { ContratoDetalleComponent } from './contrato-detalle.component';

describe('ContratoDetalleComponent', () => {
  let fixture: ComponentFixture<ContratoDetalleComponent>;
  let component: ContratoDetalleComponent;
  let informeService: jasmine.SpyObj<InformeService>;

  const contrato = sampleContrato();
  const pageInformes = samplePageInformes([sampleInforme(1), sampleInforme(2)]);
  const pageVacia = samplePageInformes([]);

  beforeEach(async () => {
    const contratoService = jasmine.createSpyObj<ContratoService>('ContratoService', ['obtenerDetalle']);
    informeService = jasmine.createSpyObj<InformeService>('InformeService', ['listarInformes']);

    contratoService.obtenerDetalle.and.returnValue(of(contrato));
    informeService.listarInformes.and.returnValue(of(pageInformes));

    await TestBed.configureTestingModule({
      imports: [ContratoDetalleComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '10' }) } } },
        { provide: ContratoService, useValue: contratoService },
        { provide: InformeService, useValue: informeService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContratoDetalleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the contract detail on init', () => {
    expect(component.contrato()?.id).toBe(10);
    expect(component.contrato()?.numero).toBe('OPS-2026-001');
  });

  it('loads the informe history for the contract', () => {
    expect(informeService.listarInformes).toHaveBeenCalledWith({ contratoId: 10, size: 50 });
    expect(component.informes().length).toBe(2);
  });

  it('shows empty state when there are no informes', () => {
    informeService.listarInformes.and.returnValue(of(pageVacia));
    fixture = TestBed.createComponent(ContratoDetalleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.informes().length).toBe(0);
  });

  it('shows the Nuevo Informe button only when contract is EN_EJECUCION', () => {
    expect(component.contrato()?.estado).toBe('EN_EJECUCION');
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Nuevo Informe');
  });
});

function sampleContrato(): ContratoDetalle {
  return {
    id: 10,
    numero: 'OPS-2026-001',
    objeto: 'Prestar servicios profesionales',
    tipo: 'OPS',
    estado: 'EN_EJECUCION',
    fechaInicio: '2026-01-01',
    fechaFin: '2026-12-31',
    valorTotal: 10000000,
    contratistaNombre: 'Juan Escandón',
    supervisorNombre: 'Luis Supervisor',
    contratista: { id: 1, email: 'juan@example.com', nombre: 'Juan Escandón', cargo: 'Profesional', rol: 'CONTRATISTA', firmaImagen: null, activo: true },
    revisor: null,
    supervisor: { id: 3, email: 'sup@example.com', nombre: 'Luis Supervisor', cargo: 'Supervisor', rol: 'SUPERVISOR', firmaImagen: null, activo: true },
    obligaciones: [{ id: 11, orden: 1, descripcion: 'Ejecutar actividades' }],
    docsAplicables: []
  };
}

function sampleInforme(id: number): InformeResumen {
  return {
    id,
    numero: id,
    contratoId: 10,
    contratoNumero: 'OPS-2026-001',
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'APROBADO',
    fechaUltimoEnvio: '2026-05-10T10:00:00',
    fechaAprobacion: '2026-05-15T14:00:00'
  };
}

function samplePageInformes(content: InformeResumen[]): Page<InformeResumen> {
  return { content, totalElements: content.length, totalPages: 1, size: 50, number: 0, first: true, last: true };
}
