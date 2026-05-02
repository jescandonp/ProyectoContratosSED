import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { InformeDetalle } from '../../../core/models/informe.model';
import { InformeService } from '../../../core/services/informe.service';
import { InformePreviewComponent } from './informe-preview.component';

describe('InformePreviewComponent', () => {
  let fixture: ComponentFixture<InformePreviewComponent>;
  let component: InformePreviewComponent;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const informeService = jasmine.createSpyObj<InformeService>('InformeService', ['obtenerDetalle']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [InformePreviewComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '501' }) } } },
        { provide: InformeService, useValue: informeService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InformePreviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders a read-only report preview', () => {
    expect(component.informe()?.id).toBe(501);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Vista Previa');
    expect(text).toContain('Informe No. 7');
    expect(text).toContain('Actividad ejecutada');
    expect(text).toContain('DOC-301');
  });

  it('returns to report detail', () => {
    component.volver();

    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
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
    estado: 'ENVIADO',
    fechaUltimoEnvio: '2026-05-02T10:00:00',
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
