import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { ContratoDetalle } from '../../../core/models/contrato.model';
import { DocumentoCatalogo } from '../../../core/models/documento-catalogo.model';
import { InformeDetalle } from '../../../core/models/informe.model';
import { Page } from '../../../core/models/page.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { ContratoService } from '../../../core/services/contrato.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { InformeFormComponent } from './informe-form.component';

describe('InformeFormComponent', () => {
  let fixture: ComponentFixture<InformeFormComponent>;
  let component: InformeFormComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let actividadService: jasmine.SpyObj<ActividadInformeService>;
  let soporteService: jasmine.SpyObj<SoporteAdjuntoService>;
  let documentoAdicionalService: jasmine.SpyObj<DocumentoAdicionalService>;
  let router: jasmine.SpyObj<Router>;

  const contrato = sampleContrato();
  const informe = sampleInformeDetalle();

  beforeEach(async () => {
    const contratoService = jasmine.createSpyObj<ContratoService>('ContratoService', ['obtenerDetalle']);
    const catalogoService = jasmine.createSpyObj<DocumentoCatalogoService>('DocumentoCatalogoService', ['listar']);
    informeService = jasmine.createSpyObj<InformeService>('InformeService', ['crearInforme', 'enviarInforme']);
    actividadService = jasmine.createSpyObj<ActividadInformeService>('ActividadInformeService', ['crear']);
    soporteService = jasmine.createSpyObj<SoporteAdjuntoService>('SoporteAdjuntoService', ['agregarUrl', 'agregarArchivo']);
    documentoAdicionalService = jasmine.createSpyObj<DocumentoAdicionalService>('DocumentoAdicionalService', ['agregar']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    contratoService.obtenerDetalle.and.returnValue(of(contrato));
    catalogoService.listar.and.returnValue(of(sampleDocumentosPage()));
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
    documentoAdicionalService.agregar.and.returnValue(of({ id: 1, idCatalogo: 301, nombreCatalogo: 'Planilla', obligatorio: true, referencia: 'DOC-1' }));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [InformeFormComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ contratoId: '10' }) } } },
        { provide: ContratoService, useValue: contratoService },
        { provide: DocumentoCatalogoService, useValue: catalogoService },
        { provide: InformeService, useValue: informeService },
        { provide: ActividadInformeService, useValue: actividadService },
        { provide: SoporteAdjuntoService, useValue: soporteService },
        { provide: DocumentoAdicionalService, useValue: documentoAdicionalService },
        { provide: SoporteAdjuntoService, useValue: soporteService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InformeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the contract obligations and OPS document catalog', () => {
    expect(component.contrato()?.id).toBe(10);
    expect(component.actividadesForm().length).toBe(2);
    expect(component.documentosForm().length).toBe(2);
  });

  it('saves a draft with one activity per obligation plus support and additional documents', () => {
    component.fechaInicio.set('2026-05-01');
    component.fechaFin.set('2026-05-31');
    component.actividadesForm.update((rows) => rows.map((row, index) => ({
      ...row,
      descripcion: `Actividad ${index + 1}`,
      porcentaje: index === 0 ? 60 : 40,
      soporteNombre: index === 0 ? 'Evidencia' : '',
      soporteUrl: index === 0 ? 'https://sed.example/soporte' : ''
    })));
    component.documentosForm.update((docs) => docs.map((doc) => ({ ...doc, referencia: `DOC-${doc.idCatalogo}` })));

    component.guardarBorrador();

    expect(informeService.crearInforme).toHaveBeenCalledWith({
      idContrato: 10,
      fechaInicio: '2026-05-01',
      fechaFin: '2026-05-31'
    });
    expect(actividadService.crear).toHaveBeenCalledTimes(2);
    expect(actividadService.crear).toHaveBeenCalledWith(501, {
      idObligacion: 11,
      descripcion: 'Actividad 1',
      porcentaje: 60
    });
    expect(soporteService.agregarUrl).toHaveBeenCalledWith(901, { nombre: 'Evidencia', url: 'https://sed.example/soporte' });
    expect(documentoAdicionalService.agregar).toHaveBeenCalledTimes(2);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
  });

  it('confirms before sending the report', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.fechaInicio.set('2026-05-01');
    component.fechaFin.set('2026-05-31');
    component.actividadesForm.update((rows) => rows.map((row) => ({ ...row, descripcion: 'Actividad realizada', porcentaje: 100 })));
    component.documentosForm.update((docs) => docs.map((doc) => ({ ...doc, referencia: `DOC-${doc.idCatalogo}` })));

    component.confirmarEnvio();

    expect(window.confirm).toHaveBeenCalled();
    expect(informeService.enviarInforme).toHaveBeenCalledWith(501);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
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

function sampleDocumentosPage(): Page<DocumentoCatalogo> {
  return {
    content: [
      { id: 301, nombre: 'Planilla', descripcion: null, obligatorio: true, tipoContrato: 'OPS' },
      { id: 302, nombre: 'Certificacion', descripcion: null, obligatorio: false, tipoContrato: 'OPS' }
    ],
    totalElements: 2,
    totalPages: 1,
    size: 100,
    number: 0,
    first: true,
    last: true
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
