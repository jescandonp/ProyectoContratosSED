import { SlicePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, forkJoin, of, switchMap } from 'rxjs';

import { EstadoInforme, InformeDetalle } from '../../../core/models/informe.model';
import { DocumentoCatalogo } from '../../../core/models/documento-catalogo.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

interface ActividadEditState {
  descripcion: string;
  porcentaje: number;
  guardando: boolean;
  error: string;
  soporteNombre: string;
  soporteUrl: string;
  soporteArchivo: File | null;
}

@Component({
  selector: 'app-informe-detalle',
  standalone: true,
  imports: [StatusChipComponent, FormsModule],
  templateUrl: './informe-detalle.component.html'
})
export class InformeDetalleComponent implements OnInit {
  readonly informe = signal<InformeDetalle | null>(null);
  readonly error = signal('');
  readonly guardandoPeriodo = signal(false);
  readonly errorPeriodo = signal('');

  // I5 — estado de edicion de actividades (solo BORRADOR)
  readonly actividadStates = signal<Map<number, ActividadEditState>>(new Map());
  readonly catalogoDocumentos = signal<DocumentoCatalogo[]>([]);
  readonly nuevoDocIdCatalogo = signal<number | null>(null);
  readonly nuevoDocReferencia = signal('');
  readonly guardandoDocumento = signal(false);
  readonly errorDocumento = signal('');

  periodoFechaInicio = '';
  periodoFechaFin = '';

  constructor(
    private readonly informeService: InformeService,
    private readonly actividadService: ActividadInformeService,
    private readonly soporteService: SoporteAdjuntoService,
    private readonly documentoAdicionalService: DocumentoAdicionalService,
    private readonly documentoCatalogoService: DocumentoCatalogoService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('ID de informe invalido.');
      return;
    }
    this.informeService.obtenerDetalle(id).subscribe({
      next: (informe) => {
        this.informe.set(informe);
        this.periodoFechaInicio = informe.fechaInicio;
        this.periodoFechaFin = informe.fechaFin;
        if (informe.estado === 'BORRADOR') {
          this.inicializarEstadoEdicion(informe);
          this.cargarCatalogo();
        }
      },
      error: () => this.error.set('No se pudo cargar el informe.')
    });
  }

  // ── Periodo editable (I4) ─────────────────────────────────────────────────

  guardarPeriodo() {
    const informe = this.informe();
    if (!informe) return;
    this.errorPeriodo.set('');
    this.guardandoPeriodo.set(true);
    this.informeService.actualizarPeriodo(informe.id, {
      fechaInicio: this.periodoFechaInicio,
      fechaFin: this.periodoFechaFin
    }).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.periodoFechaInicio = actualizado.fechaInicio;
        this.periodoFechaFin = actualizado.fechaFin;
        this.guardandoPeriodo.set(false);
      },
      error: (err) => {
        this.guardandoPeriodo.set(false);
        const codigo = err?.error?.error ?? '';
        if (codigo === 'FECHA_FIN_INVALIDA') {
          this.errorPeriodo.set('La fecha fin no puede ser anterior a la fecha inicio.');
        } else {
          this.errorPeriodo.set('No se pudo guardar el periodo. Verifique las fechas e intente de nuevo.');
        }
      }
    });
  }

  // ── Actividades editables (I5) ────────────────────────────────────────────

  private inicializarEstadoEdicion(informe: InformeDetalle): void {
    const states = new Map<number, ActividadEditState>();
    informe.actividades.forEach((actividad) => {
      states.set(actividad.id, {
        descripcion: actividad.descripcion,
        porcentaje: actividad.porcentaje,
        guardando: false,
        error: '',
        soporteNombre: '',
        soporteUrl: '',
        soporteArchivo: null
      });
    });
    this.actividadStates.set(states);
  }

  private cargarCatalogo(): void {
    this.documentoCatalogoService.listar({ tipoContrato: 'OPS', size: 100 }).subscribe({
      next: (page) => this.catalogoDocumentos.set(page.content),
      error: () => { /* catalogo no critico */ }
    });
  }

  actualizarEstadoActividad(actividadId: number, patch: Partial<ActividadEditState>): void {
    this.actividadStates.update((map) => {
      const current = map.get(actividadId);
      if (!current) return map;
      const next = new Map(map);
      next.set(actividadId, { ...current, ...patch });
      return next;
    });
  }

  getEstadoActividad(actividadId: number): ActividadEditState | undefined {
    return this.actividadStates().get(actividadId);
  }

  guardarActividad(actividadId: number): void {
    const informe = this.informe();
    if (!informe) return;
    const state = this.actividadStates().get(actividadId);
    if (!state) return;

    if (!state.descripcion.trim()) {
      this.actualizarEstadoActividad(actividadId, { error: 'La descripcion no puede estar vacia.' });
      return;
    }
    if (state.porcentaje < 0 || state.porcentaje > 100) {
      this.actualizarEstadoActividad(actividadId, { error: 'El porcentaje debe estar entre 0 y 100.' });
      return;
    }

    const actividad = informe.actividades.find((a) => a.id === actividadId);
    if (!actividad) return;

    this.actualizarEstadoActividad(actividadId, { guardando: true, error: '' });

    this.actividadService.actualizar(informe.id, actividadId, {
      idObligacion: actividad.idObligacion ?? 0,
      descripcion: state.descripcion.trim(),
      porcentaje: state.porcentaje
    }).pipe(
      switchMap((actividadActualizada) => {
        const ops: Observable<unknown>[] = [];
        const nombre = state.soporteNombre.trim() || ('Soporte obligacion ' + (actividad.ordenObligacion ?? actividadId));
        if (state.soporteUrl.trim()) {
          ops.push(this.soporteService.agregarUrl(actividadActualizada.id, { nombre: nombre, url: state.soporteUrl.trim() }));
        }
        if (state.soporteArchivo) {
          ops.push(this.soporteService.agregarArchivo(actividadActualizada.id, state.soporteArchivo));
        }
        return ops.length ? forkJoin(ops) : of([]);
      }),
      switchMap(() => this.informeService.obtenerDetalle(informe.id))
    ).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.inicializarEstadoEdicion(actualizado);
      },
      error: () => {
        this.actualizarEstadoActividad(actividadId, {
          guardando: false,
          error: 'No se pudo guardar la actividad. Intente de nuevo.'
        });
      }
    });
  }

  eliminarSoporte(actividadId: number, soporteId: number): void {
    const informe = this.informe();
    if (!informe) return;
    this.soporteService.eliminar(actividadId, soporteId).pipe(
      switchMap(() => this.informeService.obtenerDetalle(informe.id))
    ).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.inicializarEstadoEdicion(actualizado);
      },
      error: () => this.error.set('No se pudo eliminar el soporte.')
    });
  }

  seleccionarArchivoActividad(actividadId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.actualizarEstadoActividad(actividadId, { soporteArchivo: input.files?.item(0) ?? null });
  }

  // ── Documentos adicionales editables (I5) ────────────────────────────────

  agregarDocumentoAdicional(): void {
    const informe = this.informe();
    const idCatalogo = this.nuevoDocIdCatalogo();
    const referencia = this.nuevoDocReferencia().trim();
    if (!informe || !idCatalogo || !referencia) {
      this.errorDocumento.set('Seleccione el tipo de documento e ingrese la referencia.');
      return;
    }
    this.guardandoDocumento.set(true);
    this.errorDocumento.set('');
    this.documentoAdicionalService.agregar(informe.id, { idCatalogo: idCatalogo, referencia: referencia }).pipe(
      switchMap(() => this.informeService.obtenerDetalle(informe.id))
    ).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.inicializarEstadoEdicion(actualizado);
        this.nuevoDocIdCatalogo.set(null);
        this.nuevoDocReferencia.set('');
        this.guardandoDocumento.set(false);
      },
      error: () => {
        this.guardandoDocumento.set(false);
        this.errorDocumento.set('No se pudo agregar el documento. Intente de nuevo.');
      }
    });
  }

  eliminarDocumentoAdicional(documentoId: number): void {
    const informe = this.informe();
    if (!informe) return;
    this.documentoAdicionalService.eliminar(informe.id, documentoId).pipe(
      switchMap(() => this.informeService.obtenerDetalle(informe.id))
    ).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.inicializarEstadoEdicion(actualizado);
      },
      error: () => this.error.set('No se pudo eliminar el documento.')
    });
  }

  // ── Navegacion y estado ───────────────────────────────────────────────────

  verPreview() {
    const informe = this.informe();
    if (informe) void this.router.navigate(['/informes', informe.id, 'preview']);
  }

  verPdf() {
    const informe = this.informe();
    if (informe) void this.router.navigate(['/informes', informe.id, 'pdf']);
  }

  enviar() {
    const informe = this.informe();
    if (!informe || !this.puedeEnviar(informe.estado)) return;
    if (!window.confirm('Desea enviar este informe para revision?')) return;

    this.informeService.enviarInforme(informe.id).subscribe({
      next: (actualizado) => this.informe.set(actualizado),
      error: () => this.error.set('No se pudo enviar el informe.')
    });
  }

  volverAContratos() {
    void this.router.navigate(['/contratos']);
  }

  esBorrador(estado: EstadoInforme): boolean {
    return estado === 'BORRADOR';
  }

  periodoEditable(estado: EstadoInforme): boolean {
    return estado === 'BORRADOR' || estado === 'DEVUELTO';
  }

  puedeEnviar(estado: EstadoInforme) {
    return estado === 'BORRADOR' || estado === 'DEVUELTO';
  }

  toNumber(value: string | number): number {
    return Number(value) || 0;
  }

  estadoLabel(estado: EstadoInforme): string {
    const map: Record<EstadoInforme, string> = {
      BORRADOR: 'Borrador',
      ENVIADO: 'Enviado',
      EN_REVISION: 'En revision',
      DEVUELTO: 'Devuelto',
      APROBADO: 'Aprobado'
    };
    return map[estado];
  }

  estadoTone(estado: EstadoInforme): 'neutral' | 'success' | 'warning' | 'danger' {
    if (estado === 'APROBADO') return 'success';
    if (estado === 'DEVUELTO') return 'danger';
    if (estado === 'BORRADOR') return 'neutral';
    return 'warning';
  }
}
