import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { ActividadInforme } from '../../../core/models/actividad-informe.model';
import { AporteSgssiDto } from '../../../core/models/aporte-sgssi.model';
import { InformeDetalle } from '../../../core/models/informe.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { AporteSgssiService } from '../../../core/services/aporte-sgssi.service';
import { DocumentoRequeridoService } from '../../../core/services/documento-requerido.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { CorregirInformeComponent } from './corregir-informe.component';

describe('CorregirInformeComponent', () => {
  let fixture: ComponentFixture<CorregirInformeComponent>;
  let component: CorregirInformeComponent;
  let informeService: jasmine.SpyObj<InformeService>;
  let actividadService: jasmine.SpyObj<ActividadInformeService>;
  let soporteService: jasmine.SpyObj<SoporteAdjuntoService>;
  let aporteSgssiService: jasmine.SpyObj<AporteSgssiService>;
  let documentoRequeridoService: jasmine.SpyObj<DocumentoRequeridoService>;
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
    aporteSgssiService = jasmine.createSpyObj<AporteSgssiService>('AporteSgssiService', ['guardarTodos']);
    documentoRequeridoService = jasmine.createSpyObj<DocumentoRequeridoService>('DocumentoRequeridoService', [
      'listar',
      'cargarArchivo',
      'descargarArchivo',
      'eliminarArchivo'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    informeService.obtenerDetalle.and.returnValue(of(informeDevuelto));
    informeService.actualizarInforme.and.returnValue(of(informeDevuelto));
    informeService.enviarInforme.and.returnValue(of({ ...informeDevuelto, estado: 'ENVIADO' }));
    actividadService.actualizar.and.returnValue(of(sampleActividad()));
    actividadService.crear.and.returnValue(of(sampleActividad()));
    soporteService.agregarUrl.and.returnValue(of({ id: 1, tipo: 'URL', nombre: 'Soporte', referencia: 'https://example.com' }));
    aporteSgssiService.guardarTodos.and.returnValue(of([]));
    documentoRequeridoService.listar.and.returnValue(of([]));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [CorregirInformeComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '501' }) } } },
        { provide: InformeService, useValue: informeService },
        { provide: ActividadInformeService, useValue: actividadService },
        { provide: SoporteAdjuntoService, useValue: soporteService },
        { provide: AporteSgssiService, useValue: aporteSgssiService },
        { provide: DocumentoRequeridoService, useValue: documentoRequeridoService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CorregirInformeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── Tests existentes ──────────────────────────────────────────────────────

  it('loads the informe and populates the form with existing activities', () => {
    expect(component.informe()?.id).toBe(501);
    expect(component.actividadesForm().length).toBe(1);
    expect(component.actividadesForm()[0].descripcion).toBe('Actividad previa');
  });

  it('shows historical observations in the side panel', () => {
    expect(component.observaciones().length).toBe(1);
    expect(component.observaciones()[0].texto).toBe('Falta soporte en obligación 1');
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
      rows.map((row) => ({ ...row, descripcion: 'Actividad corregida' }))
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
    });
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 501]);
  });

  it('confirms before resending the corrected report', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.actividadesForm.update((rows) =>
      rows.map((row) => ({ ...row, descripcion: 'Actividad corregida' }))
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

  // ── Tests T11: aportes SGSSI ──────────────────────────────────────────────

  it('T11: precarga aportes SGSSI desde datos existentes del informe', () => {
    const informeConAportes = {
      ...informeDevuelto,
      aportesSgssi: [
        { id: 1, item: 'SALUD' as const, fechaPago: '2026-05-01', valorAportado: 100000, entidad: 'EPS Sura' }
      ]
    };
    informeService.obtenerDetalle.and.returnValue(of(informeConAportes));
    fixture = TestBed.createComponent(CorregirInformeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.aportesEdicion().length).toBe(1);
    expect(component.aportesEdicion()[0].entidad).toBe('EPS Sura');
    expect(component.aportesEdicion()[0].item).toBe('SALUD');
  });

  it('T11: precarga aportes con entidades predeterminadas del contratista cuando no hay aportes', () => {
    const informeSinAportes = {
      ...informeDevuelto,
      aportesSgssi: [],
      contratista: {
        id: 1, email: 'c@example.com', nombre: 'Contratista', cargo: null, rol: 'CONTRATISTA' as const,
        firmaImagen: null, activo: true,
        sgssiSaludEntidad: 'EPS Sura',
        sgssiPensionEntidad: 'Porvenir',
        sgssiArlEntidad: 'Positiva'
      }
    };
    informeService.obtenerDetalle.and.returnValue(of(informeSinAportes));
    fixture = TestBed.createComponent(CorregirInformeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const aportes = component.aportesEdicion();
    expect(aportes.length).toBe(3); // SALUD, PENSION, ARL
    expect(aportes.find((a) => a.item === 'SALUD')?.entidad).toBe('EPS Sura');
    expect(aportes.find((a) => a.item === 'PENSION')?.entidad).toBe('Porvenir');
    expect(aportes.find((a) => a.item === 'ARL')?.entidad).toBe('Positiva');
  });

  it('T11: permite agregar y eliminar aportes SGSSI', () => {
    const initialCount = component.aportesEdicion().length;
    component.agregarAporte();
    expect(component.aportesEdicion().length).toBe(initialCount + 1);

    component.eliminarAporte(0);
    expect(component.aportesEdicion().length).toBe(initialCount);
  });

  it('T11: guarda aportes SGSSI válidos al guardar borrador', () => {
    component.aportesEdicion.set([
      { item: 'SALUD', fechaPago: '2026-05-01', valorAportado: 100000, entidad: 'EPS Sura' }
    ]);
    component.actividadesForm.update((rows) =>
      rows.map((row) => ({ ...row, descripcion: 'Actividad corregida' }))
    );
    component.guardarBorrador();

    expect(aporteSgssiService.guardarTodos).toHaveBeenCalledWith(501, [
      jasmine.objectContaining({ item: 'SALUD', entidad: 'EPS Sura', valorAportado: 100000 })
    ]);
  });

  it('T11: no llama guardarTodos si no hay aportes válidos', () => {
    component.aportesEdicion.set([
      { item: 'SALUD', fechaPago: '', valorAportado: null, entidad: '' } // inválido
    ]);
    component.actividadesForm.update((rows) =>
      rows.map((row) => ({ ...row, descripcion: 'Actividad corregida' }))
    );
    component.guardarBorrador();

    expect(aporteSgssiService.guardarTodos).not.toHaveBeenCalled();
  });

  // ── Tests T11: documentos requeridos ─────────────────────────────────────

  it('T11: carga documentos requeridos al inicializar', () => {
    expect(documentoRequeridoService.listar).toHaveBeenCalledWith(501);
  });

  it('T11: muestra sección de documentos requeridos cuando hay documentos', () => {
    documentoRequeridoService.listar.and.returnValue(of([
      { id: null, claveLogica: 'PLANILLA', nombreDisplay: 'Planilla SGSSI', cargado: false,
        nombreArchivo: null, contentType: null, extension: null, tamanoBytes: null, porIva: false }
    ]));
    fixture = TestBed.createComponent(CorregirInformeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="seccion-documentos-requeridos"]')).not.toBeNull();
  });
});

function sampleActividad(): ActividadInforme {
  return {
    id: 901,
    idObligacion: 11,
    ordenObligacion: 1,
    descripcionObligacion: 'Ejecutar actividades del contrato',
    descripcion: 'Actividad corregida',
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
        soportes: []
      }
    ],
    documentosAdicionales: [],
    observaciones: [
      { id: 1, texto: 'Falta soporte en obligación 1', autorRol: 'SUPERVISOR', fecha: '2026-05-12T09:00:00' }
    ],
    aportesSgssi: []
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
