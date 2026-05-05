import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { InformeDetalle } from '../../../core/models/informe.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { InformeDetalleComponent } from './informe-detalle.component';

describe('InformeDetalleComponent', () => {
  let fixture: ComponentFixture<InformeDetalleComponent>;
  let component: InformeDetalleComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let actividadService: jasmine.SpyObj<ActividadInformeService>;
  let soporteService: jasmine.SpyObj<SoporteAdjuntoService>;
  let documentoAdicionalService: jasmine.SpyObj<DocumentoAdicionalService>;
  let documentoCatalogoService: jasmine.SpyObj<DocumentoCatalogoService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    informeService = jasmine.createSpyObj<InformeService>('InformeService', [
      'obtenerDetalle',
      'enviarInforme',
      'actualizarPeriodo'
    ]);
    actividadService = jasmine.createSpyObj<ActividadInformeService>('ActividadInformeService', ['actualizar']);
    soporteService = jasmine.createSpyObj<SoporteAdjuntoService>('SoporteAdjuntoService', ['agregarUrl', 'agregarArchivo', 'eliminar']);
    documentoAdicionalService = jasmine.createSpyObj<DocumentoAdicionalService>('DocumentoAdicionalService', ['agregar', 'eliminar']);
    documentoCatalogoService = jasmine.createSpyObj<DocumentoCatalogoService>('DocumentoCatalogoService', ['listar']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));
    informeService.enviarInforme.and.returnValue(of({ ...sampleInformeDetalle(), estado: 'ENVIADO' }));
    informeService.actualizarPeriodo.and.returnValue(of({ ...sampleInformeDetalle(), fechaInicio: '2026-06-01', fechaFin: '2026-06-30' }));
    actividadService.actualizar.and.returnValue(of(sampleInformeDetalle().actividades[0]));
    soporteService.agregarUrl.and.returnValue(of({ id: 99, tipo: 'URL' as const, nombre: 'Nuevo soporte', referencia: 'https://example.com' }));
    soporteService.eliminar.and.returnValue(of(void 0));
    documentoAdicionalService.agregar.and.returnValue(of({ id: 99, idCatalogo: 301, nombreCatalogo: 'Planilla', obligatorio: true, referencia: 'REF-NEW' }));
    documentoAdicionalService.eliminar.and.returnValue(of(void 0));
    documentoCatalogoService.listar.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, size: 100, number: 0, first: true, last: true }));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [InformeDetalleComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '501' }) } } },
        { provide: InformeService, useValue: informeService },
        { provide: ActividadInformeService, useValue: actividadService },
        { provide: SoporteAdjuntoService, useValue: soporteService },
        { provide: DocumentoAdicionalService, useValue: documentoAdicionalService },
        { provide: DocumentoCatalogoService, useValue: documentoCatalogoService },
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
    // En BORRADOR la descripcion esta en un textarea (modo edicion)
    const textarea = fixture.nativeElement.querySelector('[data-testid="input-descripcion-1001"]') as HTMLTextAreaElement;
    expect(textarea).not.toBeNull();
    expect(textarea.value).toContain('Actividad ejecutada');
  });

  it('navigates to the read-only preview', () => {
    component.verPreview();

    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501, 'preview']);
  });

  it('shows PDF action for approved reports with generated PDF', () => {
    component.informe.set({
      ...sampleInformeDetalle(),
      estado: 'APROBADO',
      pdfRuta: 'pdfs/10/501/informe-7.pdf'
    });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('[data-testid="ver-pdf"]').click();

    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501, 'pdf']);
  });

  it('hides PDF action for approved reports without generated PDF', () => {
    component.informe.set({
      ...sampleInformeDetalle(),
      estado: 'APROBADO',
      pdfRuta: null
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="ver-pdf"]')).toBeNull();
  });

  it('confirms before sending a draft report', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.enviar();

    expect(window.confirm).toHaveBeenCalled();
    expect(informeService.enviarInforme).toHaveBeenCalledWith(501);
    expect(component.informe()?.estado).toBe('ENVIADO');
  });

  // ── T8: H3 periodo editable ───────────────────────────────────────────────

  it('muestra campos editables de periodo en estado BORRADOR', () => {
    fixture.detectChanges();

    const periodoEditable = fixture.nativeElement.querySelector('[data-testid="periodo-editable"]');
    expect(periodoEditable).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="btn-guardar-periodo"]')).not.toBeNull();
  });

  it('no muestra campos editables de periodo en estado ENVIADO', () => {
    component.informe.set({ ...sampleInformeDetalle(), estado: 'ENVIADO' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="periodo-editable"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="btn-guardar-periodo"]')).toBeNull();
  });

  it('guarda el periodo correctamente y actualiza la vista', () => {
    component.periodoFechaInicio = '2026-06-01';
    component.periodoFechaFin = '2026-06-30';

    component.guardarPeriodo();

    expect(informeService.actualizarPeriodo).toHaveBeenCalledWith(501, {
      fechaInicio: '2026-06-01',
      fechaFin: '2026-06-30'
    });
    expect(component.informe()?.fechaInicio).toBe('2026-06-01');
    expect(component.informe()?.fechaFin).toBe('2026-06-30');
    expect(component.guardandoPeriodo()).toBeFalse();
  });

  // ── T3 I5: edicion de actividades en BORRADOR ─────────────────────────────

  it('muestra actividades en modo edicion cuando estado es BORRADOR', () => {
    fixture.detectChanges();

    const tarjetas = fixture.nativeElement.querySelectorAll('[data-testid="actividad-editable"]');
    expect(tarjetas.length).toBe(1);
    expect(fixture.nativeElement.querySelector('[data-testid="btn-guardar-actividad-1001"]')).not.toBeNull();
  });

  it('no muestra controles de edicion en estado ENVIADO', () => {
    component.informe.set({ ...sampleInformeDetalle(), estado: 'ENVIADO' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="actividad-editable"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="btn-guardar-actividad-1001"]')).toBeNull();
  });

  it('no muestra controles de edicion en estado EN_REVISION', () => {
    component.informe.set({ ...sampleInformeDetalle(), estado: 'EN_REVISION' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="actividad-editable"]')).toBeNull();
  });

  it('no muestra controles de edicion en estado APROBADO', () => {
    component.informe.set({ ...sampleInformeDetalle(), estado: 'APROBADO', pdfRuta: null });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="actividad-editable"]')).toBeNull();
  });

  it('guarda actividad correctamente y recarga el informe', () => {
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));

    component.guardarActividad(1001);

    expect(actividadService.actualizar).toHaveBeenCalledWith(501, 1001, jasmine.objectContaining({
      descripcion: 'Actividad ejecutada',
      porcentaje: 100
    }));
    expect(informeService.obtenerDetalle).toHaveBeenCalledWith(501);
  });

  it('muestra error inline si descripcion esta vacia al guardar', () => {
    component.actualizarEstadoActividad(1001, { descripcion: '' });

    component.guardarActividad(1001);

    expect(actividadService.actualizar).not.toHaveBeenCalled();
    expect(component.getEstadoActividad(1001)?.error).toContain('descripcion');
  });

  it('muestra error inline si porcentaje es invalido', () => {
    component.actualizarEstadoActividad(1001, { porcentaje: 150 });

    component.guardarActividad(1001);

    expect(actividadService.actualizar).not.toHaveBeenCalled();
    expect(component.getEstadoActividad(1001)?.error).toContain('porcentaje');
  });

  it('muestra indicador de carga durante guardado de actividad', () => {
    actividadService.actualizar.and.returnValue(new Subject());

    component.guardarActividad(1001);

    expect(component.getEstadoActividad(1001)?.guardando).toBeTrue();
  });

  it('elimina soporte y recarga el informe', () => {
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));

    component.eliminarSoporte(1001, 1);

    expect(soporteService.eliminar).toHaveBeenCalledWith(1001, 1);
    expect(informeService.obtenerDetalle).toHaveBeenCalledWith(501);
  });

  it('agrega soporte URL y recarga el informe', () => {
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));
    component.actualizarEstadoActividad(1001, { soporteUrl: 'https://example.com/soporte' });

    component.guardarActividad(1001);

    expect(soporteService.agregarUrl).toHaveBeenCalledWith(
      1001,
      jasmine.objectContaining({ url: 'https://example.com/soporte' })
    );
  });

  it('agrega documento adicional y recarga el informe', () => {
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));
    component.nuevoDocIdCatalogo.set(301);
    component.nuevoDocReferencia.set('REF-TEST');

    component.agregarDocumentoAdicional();

    expect(documentoAdicionalService.agregar).toHaveBeenCalledWith(501, { idCatalogo: 301, referencia: 'REF-TEST' });
    expect(informeService.obtenerDetalle).toHaveBeenCalledWith(501);
  });

  it('elimina documento adicional y recarga el informe', () => {
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));

    component.eliminarDocumentoAdicional(1);

    expect(documentoAdicionalService.eliminar).toHaveBeenCalledWith(501, 1);
    expect(informeService.obtenerDetalle).toHaveBeenCalledWith(501);
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
