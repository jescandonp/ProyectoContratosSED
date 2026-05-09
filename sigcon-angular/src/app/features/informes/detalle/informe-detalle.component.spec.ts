import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { AporteSgssiDto } from '../../../core/models/aporte-sgssi.model';
import { InformeDetalle } from '../../../core/models/informe.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { AporteSgssiService } from '../../../core/services/aporte-sgssi.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { InformeService } from '../../../core/services/informe.service';
import { ObservacionService } from '../../../core/services/observacion.service';
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
  let aporteSgssiService: jasmine.SpyObj<AporteSgssiService>;
  let observacionService: jasmine.SpyObj<ObservacionService>;
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
    aporteSgssiService = jasmine.createSpyObj<AporteSgssiService>('AporteSgssiService', ['guardarTodos']);
    observacionService = jasmine.createSpyObj<ObservacionService>('ObservacionService', ['aprobarRevision', 'devolverRevision']);
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
    aporteSgssiService.guardarTodos.and.returnValue(of([]));
    observacionService.aprobarRevision.and.returnValue(of({ ...sampleInformeDetalle(), estado: 'EN_REVISION' as const }));
    observacionService.devolverRevision.and.returnValue(of({ ...sampleInformeDetalle(), estado: 'DEVUELTO' as const }));
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
        { provide: AporteSgssiService, useValue: aporteSgssiService },
        { provide: ObservacionService, useValue: observacionService },
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

  // ── Periodo editable ──────────────────────────────────────────────────────

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

  // ── Edicion de actividades (I5 — sin porcentaje) ──────────────────────────

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

  it('tarjeta de actividad en BORRADOR no tiene campo de porcentaje', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="input-porcentaje-1001"]')).toBeNull();
  });

  it('tarjeta de actividad en modo solo lectura no muestra badge de porcentaje', () => {
    component.informe.set({ ...sampleInformeDetalle(), estado: 'ENVIADO' });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).not.toMatch(/\d+%/);
  });

  it('guarda actividad correctamente y recarga el informe', () => {
    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));

    component.guardarActividad(1001);

    expect(actividadService.actualizar).toHaveBeenCalledWith(501, 1001, jasmine.objectContaining({
      descripcion: 'Actividad ejecutada'
    }));
    expect(informeService.obtenerDetalle).toHaveBeenCalledWith(501);
  });

  it('muestra error inline si descripcion esta vacia al guardar', () => {
    component.actualizarEstadoActividad(1001, { descripcion: '' });

    component.guardarActividad(1001);

    expect(actividadService.actualizar).not.toHaveBeenCalled();
    expect(component.getEstadoActividad(1001)?.error).toContain('descripcion');
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

  // ── I6: Aportes SGSSI ────────────────────────────────────────────────────

  it('muestra la seccion SGSSI siempre visible', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="seccion-sgssi"]')).not.toBeNull();
  });

  it('muestra boton agregar aporte en estado BORRADOR', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="btn-agregar-aporte"]')).not.toBeNull();
  });

  it('no muestra boton agregar aporte en estado ENVIADO', () => {
    component.informe.set({ ...sampleInformeDetalle(), estado: 'ENVIADO' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="btn-agregar-aporte"]')).toBeNull();
  });

  it('muestra mensaje vacio si no hay aportes en BORRADOR', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="sgssi-vacio"]')).not.toBeNull();
  });

  it('puede agregar una fila de aporte en modo edicion', () => {
    component.agregarAporteEdicion();

    expect(component.aportesEdicion().length).toBe(1);
    expect(component.aportesEdicion()[0].item).toBe('SALUD');
  });

  it('puede eliminar una fila de aporte en modo edicion', () => {
    component.agregarAporteEdicion();
    component.agregarAporteEdicion();
    expect(component.aportesEdicion().length).toBe(2);

    component.eliminarAporteEdicion(0);

    expect(component.aportesEdicion().length).toBe(1);
  });

  it('puede actualizar campos de una fila de aporte', () => {
    component.agregarAporteEdicion();
    component.actualizarAporteEdicion(0, { item: 'ARL', entidad: 'Sura' });

    expect(component.aportesEdicion()[0].item).toBe('ARL');
    expect(component.aportesEdicion()[0].entidad).toBe('Sura');
  });

  it('guarda aportes SGSSI validos y recarga el informe', () => {
    const recargado = { ...sampleInformeDetalle(), aportesSgssi: [sampleAporte()] };
    informeService.obtenerDetalle.and.returnValue(of(recargado));

    component.agregarAporteEdicion();
    component.actualizarAporteEdicion(0, { item: 'SALUD', entidad: 'Sanitas', fechaPago: '2026-04-05', valorAportado: 150000 });
    component.guardarAportesSgssi();

    expect(aporteSgssiService.guardarTodos).toHaveBeenCalledWith(501, jasmine.arrayContaining([
      jasmine.objectContaining({ item: 'SALUD', entidad: 'Sanitas', fechaPago: '2026-04-05', valorAportado: 150000 })
    ]));
    expect(informeService.obtenerDetalle).toHaveBeenCalledWith(501);
  });

  it('ignora filas de aporte incompletas al guardar', () => {
    component.agregarAporteEdicion();
    // La fila tiene fechaPago vacía y valorAportado null — no debe enviarse

    component.guardarAportesSgssi();

    expect(aporteSgssiService.guardarTodos).toHaveBeenCalledWith(501, []);
  });

  it('muestra error si el guardado de aportes falla', () => {
    aporteSgssiService.guardarTodos.and.returnValue(throwError(() => new Error('fail')));

    component.guardarAportesSgssi();

    expect(component.errorAportes()).toContain('No se pudieron guardar');
    expect(component.guardandoAportes()).toBeFalse();
  });

  it('muestra boton guardar aportes en estado BORRADOR', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="btn-guardar-aportes"]')).not.toBeNull();
  });

  it('muestra aportes en modo solo lectura en estado APROBADO', () => {
    const aprobado = {
      ...sampleInformeDetalle(),
      estado: 'APROBADO' as const,
      pdfRuta: null,
      aportesSgssi: [sampleAporte()]
    };
    component.informe.set(aprobado);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Salud');
    expect(text).toContain('Sanitas');
  });

  it('inicializa aportesEdicion con los aportes existentes del informe al cargar', () => {
    const conAportes = { ...sampleInformeDetalle(), aportesSgssi: [sampleAporte()] };
    informeService.obtenerDetalle.and.returnValue(of(conAportes));

    component.cargar();

    expect(component.aportesEdicion().length).toBe(1);
    expect(component.aportesEdicion()[0].item).toBe('SALUD');
    expect(component.aportesEdicion()[0].entidad).toBe('Sanitas');
  });

  it('labelSgssi retorna etiqueta legible para cada item', () => {
    expect(component.labelSgssi('SALUD')).toBe('Salud');
    expect(component.labelSgssi('PENSION')).toBe('Pensión');
    expect(component.labelSgssi('ARL')).toBe('A.R.L.');
  });
});

// ── Fixtures ──────────────────────────────────────────────────────────────────

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
    contratista: {
      id: 1, email: 'ana@example.com', nombre: 'Ana Contratista', cargo: 'Profesional',
      rol: 'CONTRATISTA', firmaImagen: null, activo: true,
      sgssiSaludEntidad: null, sgssiPensionEntidad: null, sgssiArlEntidad: null
    },
    revisor: null,
    supervisor: {
      id: 3, email: 'sup@example.com', nombre: 'Luis Supervisor', cargo: 'Supervisor',
      rol: 'SUPERVISOR', firmaImagen: null, activo: true,
      sgssiSaludEntidad: null, sgssiPensionEntidad: null, sgssiArlEntidad: null
    },
    actividades: [
      {
        id: 1001,
        idObligacion: 11,
        ordenObligacion: 1,
        descripcionObligacion: 'Ejecutar actividades del contrato',
        descripcion: 'Actividad ejecutada',
        soportes: [{ id: 1, tipo: 'URL', nombre: 'Evidencia', referencia: 'https://sed.example/soporte' }]
      }
    ],
    documentosAdicionales: [{ id: 1, idCatalogo: 301, nombreCatalogo: 'Planilla', obligatorio: true, referencia: 'DOC-301' }],
    observaciones: [],
    aportesSgssi: [],
    numeroDesembolso: null,
    valorDesembolso: null,
    porcentajeEjecucion: null,
    correspondenciaPendiente: null,
  };
}

function sampleAporte(): AporteSgssiDto {
  return {
    id: 1,
    item: 'SALUD',
    entidad: 'Sanitas',
    fechaPago: '2026-04-05',
    valorAportado: 150000,
  };
}
