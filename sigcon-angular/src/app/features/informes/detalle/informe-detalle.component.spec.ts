import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { InformeDetalle } from '../../../core/models/informe.model';
import { InformeService } from '../../../core/services/informe.service';
import { InformeDetalleComponent } from './informe-detalle.component';

describe('InformeDetalleComponent', () => {
  let fixture: ComponentFixture<InformeDetalleComponent>;
  let component: InformeDetalleComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    informeService = jasmine.createSpyObj<InformeService>('InformeService', ['obtenerDetalle', 'enviarInforme']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));
    informeService.enviarInforme.and.returnValue(of({ ...sampleInformeDetalle(), estado: 'ENVIADO' }));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [InformeDetalleComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '501' }) } } },
        { provide: InformeService, useValue: informeService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InformeDetalleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads and renders the report detail', () => {
    expect(component.informe()?.id).toBe(501);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Informe No. 7');
    expect(text).toContain('OPS-2026-001');
    expect(text).toContain('Actividad ejecutada');
  });

  it('navigates to the read-only preview', () => {
    component.verPreview();

    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501, 'preview']);
  });

  it('confirms before sending a draft report', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.enviar();

    expect(window.confirm).toHaveBeenCalled();
    expect(informeService.enviarInforme).toHaveBeenCalledWith(501);
    expect(component.informe()?.estado).toBe('ENVIADO');
  });
});

function sampleInformeDetalle(): InformeDetalle {
  return {
    id: 501,
    numero: 7,
    contratoId: 10,
    contratoNumero: 'OPS-2026-001',
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'BORRADOR',
    fechaUltimoEnvio: null,
    fechaAprobacion: null,
    contratista: { id: 1, email: 'ana@example.com', nombre: 'Ana Contratista', cargo: 'Profesional', rol: 'CONTRATISTA', firmaImagen: null, activo: true },
    revisor: null,
    supervisor: { id: 3, email: 'sup@example.com', nombre: 'Luis Supervisor', cargo: 'Supervisor', rol: 'SUPERVISOR', firmaImagen: null, activo: true },
    actividades: [
      {
        id: 1001,
        idObligacion: 11,
        ordenObligacion: 1,
        descripcionObligacion: 'Ejecutar actividades del contrato',
        descripcion: 'Actividad ejecutada',
        porcentaje: 100,
        soportes: [{ id: 1, tipo: 'URL', nombre: 'Evidencia', referencia: 'https://sed.example/soporte' }]
      }
    ],
    documentosAdicionales: [{ id: 1, idCatalogo: 301, nombreCatalogo: 'Planilla', obligatorio: true, referencia: 'DOC-301' }],
    observaciones: []
  };
}
