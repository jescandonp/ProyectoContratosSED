import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { ActividadInforme } from '../../../core/models/actividad-informe.model';
import { DocumentoCatalogo } from '../../../core/models/documento-catalogo.model';
import { InformeDetalle } from '../../../core/models/informe.model';
import { Page } from '../../../core/models/page.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { CorregirInformeComponent } from './corregir-informe.component';

describe('CorregirInformeComponent', () => {
  let fixture: ComponentFixture<CorregirInformeComponent>;
  let component: CorregirInformeComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let actividadService: jasmine.SpyObj<ActividadInformeService>;
  let soporteService: jasmine.SpyObj<SoporteAdjuntoService>;
  let documentoAdicionalService: jasmine.SpyObj<DocumentoAdicionalService>;
  let router: jasmine.SpyObj<Router>;

  const informeDevuelto = sampleInformeDevuelto();
  const informeAprobado = sampleInformeAprobado();

  beforeEach(async () => {
    informeService = jasmine.createSpyObj<InformeService>('InformeService', [
      'obtenerDetalle',
      'actualizarInforme',
      'enviarInforme'
    ]);
    actividadService = jasmine.createSpyObj<ActividadInformeService>('ActividadInformeService', [
      'crear',
      'actualizar'
    ]);
    soporteService = jasmine.createSpyObj<SoporteAdjuntoService>('SoporteAdjuntoService', [
      'agregarUrl',
      'agregarArchivo'
    ]);
    documentoAdicionalService = jasmine.createSpyObj<DocumentoAdicionalService>('DocumentoAdicionalService', [
      'agregar'
    ]);
    const catalogoService = jasmine.createSpyObj<DocumentoCatalogoService>('DocumentoCatalogoService', ['listar']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    informeService.obtenerDetalle.and.returnValue(of(informeDevuelto));
    informeService.actualizarInforme.and.returnValue(of(informeDevuelto));
    informeService.enviarInforme.and.returnValue(of({ ...informeDevuelto, estado: 'ENVIADO' }));
    actividadService.actualizar.and.returnValue(of(sampleActividad()));
    actividadService.crear.and.returnValue(of(sampleActividad()));
    soporteService.agregarUrl.and.returnValue(of({ id: 1, tipo: 'URL', nombre: 'Soporte', referencia: 'https://example.com' }));
    documentoAdicionalService.agregar.and.returnValue(of({ id: 1, idCatalogo: 301, nombreCatalogo: 'Planilla', obligatorio: true, referencia: 'DOC-1' }));
    catalogoService.listar.and.returnValue(of(sampleCatalogoPage()));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [CorregirInformeComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '501' }) } } },
        { provide: InformeService, useValue: informeService },
        { provide: ActividadInformeService, useValue: actividadService },
        { provide: SoporteAdjuntoService, useValue: soporteService },
        { provide: DocumentoAdicionalService, useValue: documentoAdicionalService },
        { provide: DocumentoCatalogoService, useValue: catalogoService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CorregirInformeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the informe and populates the form with existing activities', () => {
    expect(component.informe()?.id).toBe(501);
    expect(component.actividadesForm().length).toBe(1);
    expect(component.actividadesForm()[0].descripcion).toBe('Actividad previa');
    expect(component.actividadesForm()[0].porcentaje).toBe(50);
  });

  it('shows historical observations in the side panel', () => {
    expect(component.observaciones().length).toBe(1);
    expect(component.observaciones()[0].texto).toBe('Falta soporte en obligación 1');
  });

  it('pre-fills existing document references from the informe', () => {
    expect(component.documentosForm().length).toBe(2);
    const planilla = component.documentosForm().find((d) => d.idCatalogo === 301);
    expect(planilla?.referencia).toBe('DOC-EXISTENTE');
  });

  it('does not allow editing when estado is not BORRADOR or DEVUELTO', async () => {
    informeService.obtenerDetalle.and.returnValue(of(informeAprobado));
    fixture = TestBed.createComponent(CorregirInformeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.estadoInvalido()).toBeTrue();
  });

  it('saves corrections by updating existing activities', () => {
    component.actividadesForm.update((rows) =>
      rows.map((row) => ({ ...row, descripcion: 'Actividad corregida', porcentaje: 80 }))
    );
    component.documentosForm.update((docs) =>
      docs.map((doc) => ({ ...doc, referencia: doc.referencia || `REF-${doc.idCatalogo}` }))
    );

    component.guardarBorrador();

    expect(informeService.actualizarInforme).toHaveBeenCalledWith(501, jasmine.objectContaining({
      idContrato: 10,
      fechaInicio: '2026-05-01',
      fechaFin: '2026-05-31'
    }));
    expect(actividadService.actualizar).toHaveBeenCalledWith(501, 901, {
      idObligacion: 11,
      descripcion: 'Actividad corregida',
      porcentaje: 80
    });
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
  });

  it('confirms before resending the corrected report', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.actividadesForm.update((rows) =>
      rows.map((row) => ({ ...row, descripcion: 'Actividad corregida', porcentaje: 80 }))
    );
    component.documentosForm.update((docs) =>
      docs.map((doc) => ({ ...doc, referencia: doc.referencia || `REF-${doc.idCatalogo}` }))
    );

    component.confirmarReenvio();

    expect(window.confirm).toHaveBeenCalled();
    expect(informeService.enviarInforme).toHaveBeenCalledWith(501);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
  });

  it('blocks resend confirmation when user cancels the dialog', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.confirmarReenvio();
    expect(informeService.enviarInforme).not.toHaveBeenCalled();
  });
});

function sampleActividad(): ActividadInforme {
  return {
    id: 901,
    idObligacion: 11,
    ordenObligacion: 1,
    descripcionObligacion: 'Ejecutar actividades del contrato',
    descripcion: 'Actividad corregida',
    porcentaje: 80,
    soportes: []
  };
}

function sampleInformeDevuelto(): InformeDetalle {
  return {
    id: 501,
    numero: 7,
    contratoId: 10,
    contratoNumero: 'OPS-2026-001',
    fechaInicio: '2026-05-01',
    fechaFin: '2026-05-31',
    estado: 'DEVUELTO',
    fechaUltimoEnvio: '2026-05-10T10:00:00',
    fechaAprobacion: null,
    contratista: null,
    revisor: { id: 2, email: 'rev@example.com', nombre: 'Rita Revisora', cargo: 'Revisora', rol: 'REVISOR', firmaImagen: null, activo: true },
    supervisor: { id: 3, email: 'sup@example.com', nombre: 'Luis Supervisor', cargo: 'Supervisor', rol: 'SUPERVISOR', firmaImagen: null, activo: true },
    actividades: [
      {
        id: 901,
        idObligacion: 11,
        ordenObligacion: 1,
        descripcionObligacion: 'Ejecutar actividades del contrato',
        descripcion: 'Actividad previa',
        porcentaje: 50,
        soportes: []
      }
    ],
    documentosAdicionales: [
      { id: 1, idCatalogo: 301, nombreCatalogo: 'Planilla', obligatorio: true, referencia: 'DOC-EXISTENTE' }
    ],
    observaciones: [
      { id: 1, texto: 'Falta soporte en obligación 1', autorRol: 'SUPERVISOR', fecha: '2026-05-12T09:00:00' }
    ]
  };
}

function sampleInformeAprobado(): InformeDetalle {
  return {
    id: 502,
    numero: 8,
    contratoId: 10,
    contratoNumero: 'OPS-2026-001',
    fechaInicio: '2026-06-01',
    fechaFin: '2026-06-30',
    estado: 'APROBADO',
    fechaUltimoEnvio: '2026-06-10T10:00:00',
    fechaAprobacion: '2026-06-15T14:00:00',
    contratista: null,
    revisor: null,
    supervisor: null,
    actividades: [],
    documentosAdicionales: [],
    observaciones: []
  };
}

function sampleCatalogoPage(): Page<DocumentoCatalogo> {
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
