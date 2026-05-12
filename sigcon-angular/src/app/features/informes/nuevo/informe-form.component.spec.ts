import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { ContratoDetalle } from '../../../core/models/contrato.model';
import { InformeDetalle } from '../../../core/models/informe.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { AporteSgssiService } from '../../../core/services/aporte-sgssi.service';
import { ContratoService } from '../../../core/services/contrato.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { InformeFormComponent } from './informe-form.component';

describe('InformeFormComponent', () => {
  let fixture: ComponentFixture<InformeFormComponent>;
  let component: InformeFormComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let actividadService: jasmine.SpyObj<ActividadInformeService>;
  let soporteService: jasmine.SpyObj<SoporteAdjuntoService>;
  let router: jasmine.SpyObj<Router>;

  const contrato = sampleContrato();
  const informe = sampleInformeDetalle();

  beforeEach(async () => {
    const contratoService = jasmine.createSpyObj<ContratoService>('ContratoService', ['obtenerDetalle']);
    const aporteSgssiService = jasmine.createSpyObj<AporteSgssiService>('AporteSgssiService', ['guardarTodos']);
    informeService = jasmine.createSpyObj<InformeService>('InformeService', ['crearInforme', 'enviarInforme']);
    actividadService = jasmine.createSpyObj<ActividadInformeService>('ActividadInformeService', ['crear']);
    soporteService = jasmine.createSpyObj<SoporteAdjuntoService>('SoporteAdjuntoService', ['agregarUrl']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    contratoService.obtenerDetalle.and.returnValue(of(contrato));
    informeService.crearInforme.and.returnValue(of(informe));
    informeService.enviarInforme.and.returnValue(of({ ...informe, estado: 'ENVIADO' }));
    actividadService.crear.and.returnValue(of({
      id: 901,
      idObligacion: 11,
      ordenObligacion: 1,
      descripcionObligacion: 'Ejecutar actividades del contrato',
      descripcion: 'Actividad realizada',
      porcentaje: 50,
      soportes: []
    }));
    soporteService.agregarUrl.and.returnValue(of({ id: 1, tipo: 'URL', nombre: 'Evidencia', referencia: 'https://sed.example/soporte' }));
    aporteSgssiService.guardarTodos.and.returnValue(of([]));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [InformeFormComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ contratoId: '10' }) } } },
        { provide: ContratoService, useValue: contratoService },
        { provide: InformeService, useValue: informeService },
        { provide: ActividadInformeService, useValue: actividadService },
        { provide: SoporteAdjuntoService, useValue: soporteService },
        { provide: AporteSgssiService, useValue: aporteSgssiService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InformeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the contract obligations', () => {
    expect(component.contrato()?.id).toBe(10);
    expect(component.actividadesForm().length).toBe(2);
  });

  it('saves a draft with one activity per obligation plus support', () => {
    component.fechaInicio.set('2026-05-01');
    component.fechaFin.set('2026-05-31');
    component.actividadesForm.update((rows) => rows.map((row, index) => ({
      ...row,
      descripcion: `Actividad ${index + 1}`,
      soporteNombre: `Evidencia ${index + 1}`,
      soporteUrl: `https://sed.example/soporte-${index + 1}`
    })));
    component.guardarBorrador();

    expect(informeService.crearInforme).toHaveBeenCalledWith(jasmine.objectContaining({
      idContrato: 10,
      fechaInicio: '2026-05-01',
      fechaFin: '2026-05-31'
    }));
    expect(actividadService.crear).toHaveBeenCalledTimes(2);
    expect(actividadService.crear).toHaveBeenCalledWith(501, {
      idObligacion: 11,
      descripcion: 'Actividad 1'
    });
    expect(soporteService.agregarUrl).toHaveBeenCalledTimes(2);
    expect(soporteService.agregarUrl).toHaveBeenCalledWith(901, { nombre: 'Evidencia 1', url: 'https://sed.example/soporte-1' });
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
  });

  it('confirms before sending the report', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.fechaInicio.set('2026-05-01');
    component.fechaFin.set('2026-05-31');
    component.actividadesForm.update((rows) => rows.map((row, index) => ({
      ...row,
      descripcion: 'Actividad realizada',
      soporteNombre: `Evidencia ${index + 1}`,
      soporteUrl: `https://sed.example/soporte-${index + 1}`
    })));
    component.confirmarEnvio();

    expect(window.confirm).toHaveBeenCalled();
    expect(informeService.enviarInforme).toHaveBeenCalledWith(501);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
  });

  it('requires period dates before saving', () => {
    // fechaInicio y fechaFin vacíos — validación activa
    component.fechaInicio.set('');
    component.fechaFin.set('');

    component.guardarBorrador();

    expect(component.error()).toContain('periodo');
    expect(informeService.crearInforme).not.toHaveBeenCalled();
  });

  it('requires activity description before saving', () => {
    component.fechaInicio.set('2026-05-01');
    component.fechaFin.set('2026-05-31');
    // Deja descripción vacía en todas las actividades
    component.actividadesForm.update((rows) => rows.map((row) => ({ ...row, descripcion: '' })));

    component.guardarBorrador();

    expect(component.error()).toContain('actividad');
    expect(informeService.crearInforme).not.toHaveBeenCalled();
  });
});

function sampleContrato(): ContratoDetalle {
  return {
    id: 10,
    numero: 'OPS-2026-001',
    objeto: 'Prestar servicios profesionales',
    tipo: 'OPS',
    estado: 'EN_EJECUCION',
    fechaInicio: '2026-05-01',
    fechaFin: '2026-12-31',
    valorTotal: 10000000,
    contratistaNombre: 'Ana Contratista',
    supervisorNombre: 'Luis Supervisor',
    contratista: { id: 1, email: 'ana@example.com', nombre: 'Ana Contratista', cargo: 'Profesional', rol: 'CONTRATISTA', firmaImagen: null, activo: true },
    revisor: { id: 2, email: 'rev@example.com', nombre: 'Rita Revisora', cargo: 'Revisora', rol: 'REVISOR', firmaImagen: null, activo: true },
    supervisor: { id: 3, email: 'sup@example.com', nombre: 'Luis Supervisor', cargo: 'Supervisor', rol: 'SUPERVISOR', firmaImagen: null, activo: true },
    obligaciones: [
      { id: 11, orden: 1, descripcion: 'Ejecutar actividades del contrato' },
      { id: 12, orden: 2, descripcion: 'Entregar soportes mensuales' }
    ],
    docsAplicables: []
  };
}

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
    contratista: null,
    revisor: null,
    supervisor: null,
    actividades: [],
    documentosAdicionales: [],
    observaciones: []
  };
}
