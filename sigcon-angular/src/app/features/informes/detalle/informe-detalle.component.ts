import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, forkJoin, of, switchMap } from 'rxjs';

import { EstadoInforme, InformeDetalle } from '../../../core/models/informe.model';
import { AporteSgssiRequest, ITEM_SGSSI_LABELS, ItemSgssi } from '../../../core/models/aporte-sgssi.model';
import { DocumentoRequerido, EmlPreview, EXTENSIONES_PERMITIDAS_REQUERIDOS } from '../../../core/models/documento-requerido.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { AporteSgssiService } from '../../../core/services/aporte-sgssi.service';
import { AuthService } from '../../../core/auth/auth.service';
import { DocumentoRequeridoService } from '../../../core/services/documento-requerido.service';
import { InformeService } from '../../../core/services/informe.service';
import { ObservacionService } from '../../../core/services/observacion.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

interface ActividadEditState {
  descripcion: string;
  guardando: boolean;
  error: string;
  soporteNombre: string;
  soporteUrl: string;
  soporteArchivo: File | null;
}

interface AporteSgssiEditRow {
  item: ItemSgssi;
  fechaPago: string;
  valorAportado: number | null;
  entidad: string;
}

const ITEMS_SGSSI: ItemSgssi[] = ['SALUD', 'PENSION', 'ARL'];

@Component({
  selector: 'app-informe-detalle',
  standalone: true,
  imports: [StatusChipComponent, FormsModule, DecimalPipe],
  templateUrl: './informe-detalle.component.html'
})
export class InformeDetalleComponent implements OnInit {
  readonly itemsSgssi: ItemSgssi[] = ITEMS_SGSSI;

  readonly informe = signal<InformeDetalle | null>(null);
  readonly error = signal('');
  readonly guardandoPeriodo = signal(false);
  readonly errorPeriodo = signal('');

  // I5 — estado de edicion de actividades (solo BORRADOR)
  readonly actividadStates = signal<Map<number, ActividadEditState>>(new Map());
  // I6 — aportes SGSSI editables (solo BORRADOR)
  readonly aportesEdicion = signal<AporteSgssiEditRow[]>([]);
  readonly guardandoAportes = signal(false);
  readonly errorAportes = signal('');

  // I7 — documentos requeridos
  readonly documentosRequeridos = signal<DocumentoRequerido[]>([]);
  readonly cargandoRequeridos = signal(false);
  readonly errorRequeridos = signal('');
  readonly emlPreviewActivo = signal<{ documentoId: number; preview: EmlPreview } | null>(null);
  readonly cargandoArchivo = signal<string | null>(null); // claveLogica en curso

  // Flujo REVISOR — diálogo de devolución con observación
  readonly dialogoDevolucionRevision = signal(false);
  readonly observacionRevision = signal('');
  readonly procesandoRevision = signal(false);
  readonly errorRevision = signal('');

  readonly dialogoDevolucionSupervisor = signal(false);
  readonly observacionSupervisor = signal('');
  readonly procesandoSupervisor = signal(false);
  readonly errorSupervisor = signal('');

  periodoFechaInicio = '';
  periodoFechaFin = '';

  constructor(
    private readonly informeService: InformeService,
    private readonly actividadService: ActividadInformeService,
    private readonly authService: AuthService,
    private readonly soporteService: SoporteAdjuntoService,
    private readonly aporteSgssiService: AporteSgssiService,
    private readonly observacionService: ObservacionService,
    private readonly documentoRequeridoService: DocumentoRequeridoService,
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
          this.inicializarAportesEdicion(informe);
        }
        this.cargarDocumentosRequeridos(id);
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
        guardando: false,
        error: '',
        soporteNombre: '',
        soporteUrl: '',
        soporteArchivo: null
      });
    });
    this.actividadStates.set(states);
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

    const actividad = informe.actividades.find((a) => a.id === actividadId);
    if (!actividad) return;

    this.actualizarEstadoActividad(actividadId, { guardando: true, error: '' });

    this.actividadService.actualizar(informe.id, actividadId, {
      idObligacion: actividad.idObligacion ?? 0,
      descripcion: state.descripcion.trim(),
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

  // ── Aportes SGSSI editables (I6) ─────────────────────────────────────────

  private inicializarAportesEdicion(informe: InformeDetalle): void {
    const rows: AporteSgssiEditRow[] = (informe.aportesSgssi ?? []).map((a) => ({
      item: a.item,
      fechaPago: a.fechaPago,
      valorAportado: a.valorAportado,
      entidad: a.entidad,
    }));
    this.aportesEdicion.set(rows);
  }

  agregarAporteEdicion(): void {
    this.aportesEdicion.update((rows) => [...rows, { item: 'SALUD', fechaPago: '', valorAportado: null, entidad: '' }]);
  }

  eliminarAporteEdicion(index: number): void {
    this.aportesEdicion.update((rows) => rows.filter((_, i) => i !== index));
  }

  actualizarAporteEdicion(index: number, patch: Partial<AporteSgssiEditRow>): void {
    this.aportesEdicion.update((rows) => rows.map((row, i) => i === index ? { ...row, ...patch } : row));
  }

  guardarAportesSgssi(): void {
    const informe = this.informe();
    if (!informe) return;
    this.guardandoAportes.set(true);
    this.errorAportes.set('');

    const validos: AporteSgssiRequest[] = this.aportesEdicion()
      .filter((row) => row.fechaPago && row.valorAportado != null && row.entidad.trim())
      .map((row) => ({
        item: row.item,
        fechaPago: row.fechaPago,
        valorAportado: row.valorAportado!,
        entidad: row.entidad.trim(),
      }));

    this.aporteSgssiService.guardarTodos(informe.id, validos).pipe(
      switchMap(() => this.informeService.obtenerDetalle(informe.id))
    ).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.inicializarAportesEdicion(actualizado);
        this.guardandoAportes.set(false);
      },
      error: () => {
        this.guardandoAportes.set(false);
        this.errorAportes.set('No se pudieron guardar los aportes SGSSI. Intente de nuevo.');
      }
    });
  }

  labelSgssi(item: ItemSgssi): string {
    return ITEM_SGSSI_LABELS[item];
  }

  // ── Flujo REVISOR ────────────────────────────────────────────────────────

  puedeRevisar(informe: InformeDetalle): boolean {
    return this.authService.hasRole('REVISOR') && informe.estado === 'ENVIADO';
  }

  aprobarRevision(): void {
    const informe = this.informe();
    if (!informe) return;
    this.procesandoRevision.set(true);
    this.errorRevision.set('');
    this.observacionService.aprobarRevision(informe.id).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.procesandoRevision.set(false);
      },
      error: () => {
        this.procesandoRevision.set(false);
        this.errorRevision.set('No se pudo aprobar la revisión. Intente de nuevo.');
      }
    });
  }

  abrirDevolucionRevision(): void {
    this.observacionRevision.set('');
    this.errorRevision.set('');
    this.dialogoDevolucionRevision.set(true);
  }

  cerrarDevolucionRevision(): void {
    this.dialogoDevolucionRevision.set(false);
    this.errorRevision.set('');
  }

  confirmarDevolucionRevision(): void {
    const informe = this.informe();
    const texto = this.observacionRevision().trim();
    if (!informe) return;
    if (!texto) {
      this.errorRevision.set('La observación es obligatoria para devolver el informe.');
      return;
    }
    this.procesandoRevision.set(true);
    this.errorRevision.set('');
    this.observacionService.devolverRevision(informe.id, { texto }).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.procesandoRevision.set(false);
        this.dialogoDevolucionRevision.set(false);
      },
      error: () => {
        this.procesandoRevision.set(false);
        this.errorRevision.set('No se pudo devolver el informe. Intente de nuevo.');
      }
    });
  }

  puedeAprobarSupervisor(informe: InformeDetalle): boolean {
    return this.authService.hasRole('SUPERVISOR') && informe.estado === 'EN_REVISION';
  }

  aprobarSupervisor(): void {
    const informe = this.informe();
    if (!informe) return;
    this.procesandoSupervisor.set(true);
    this.errorSupervisor.set('');
    this.observacionService.aprobarInforme(informe.id).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.procesandoSupervisor.set(false);
      },
      error: () => {
        this.procesandoSupervisor.set(false);
        this.errorSupervisor.set('No se pudo aprobar el informe. Intente de nuevo.');
      }
    });
  }

  abrirDevolucionSupervisor(): void {
    this.observacionSupervisor.set('');
    this.errorSupervisor.set('');
    this.dialogoDevolucionSupervisor.set(true);
  }

  cerrarDevolucionSupervisor(): void {
    this.dialogoDevolucionSupervisor.set(false);
    this.errorSupervisor.set('');
  }

  confirmarDevolucionSupervisor(): void {
    const informe = this.informe();
    const texto = this.observacionSupervisor().trim();
    if (!informe) return;
    if (!texto) {
      this.errorSupervisor.set('La observación es obligatoria para devolver el informe.');
      return;
    }
    this.procesandoSupervisor.set(true);
    this.errorSupervisor.set('');
    this.observacionService.devolverInforme(informe.id, { texto }).subscribe({
      next: (actualizado) => {
        this.informe.set(actualizado);
        this.procesandoSupervisor.set(false);
        this.dialogoDevolucionSupervisor.set(false);
      },
      error: () => {
        this.procesandoSupervisor.set(false);
        this.errorSupervisor.set('No se pudo devolver el informe. Intente de nuevo.');
      }
    });
  }

  // ── Documentos requeridos (I7) ───────────────────────────────────────────

  private cargarDocumentosRequeridos(informeId: number): void {
    this.cargandoRequeridos.set(true);
    this.documentoRequeridoService.listar(informeId).subscribe({
      next: (docs) => {
        this.documentosRequeridos.set(docs);
        this.cargandoRequeridos.set(false);
      },
      error: () => {
        this.cargandoRequeridos.set(false);
        // No bloquear la vista si falla la carga de requeridos
      }
    });
  }

  seleccionarArchivoRequerido(claveLogica: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.item(0);
    if (!archivo) return;

    // Validar extensión en frontend antes de enviar
    const nombre = archivo.name.toLowerCase();
    const extensionValida = EXTENSIONES_PERMITIDAS_REQUERIDOS.some((ext) => nombre.endsWith(ext));
    if (!extensionValida) {
      this.errorRequeridos.set('Solo se permiten archivos PDF y EML.');
      input.value = '';
      return;
    }

    const informe = this.informe();
    if (!informe) return;

    this.errorRequeridos.set('');
    this.cargandoArchivo.set(claveLogica);

    this.documentoRequeridoService.cargarArchivo(informe.id, claveLogica, archivo).subscribe({
      next: (actualizado) => {
        this.documentosRequeridos.update((docs) => {
          const idx = docs.findIndex((d) => d.claveLogica === claveLogica);
          if (idx >= 0) {
            const copia = [...docs];
            copia[idx] = actualizado;
            return copia;
          }
          return [...docs, actualizado];
        });
        this.cargandoArchivo.set(null);
        input.value = '';
      },
      error: () => {
        this.cargandoArchivo.set(null);
        this.errorRequeridos.set('No se pudo cargar el archivo. Verifique el formato e intente de nuevo.');
        input.value = '';
      }
    });
  }

  descargarArchivoRequerido(documentoId: number, nombreArchivo: string | null): void {
    const informe = this.informe();
    if (!informe || !documentoId) return;

    this.documentoRequeridoService.descargarArchivo(informe.id, documentoId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nombreArchivo ?? 'documento';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.errorRequeridos.set('No se pudo descargar el archivo.')
    });
  }

  verPreviewEml(documentoId: number): void {
    const informe = this.informe();
    if (!informe || !documentoId) return;

    this.documentoRequeridoService.previewEml(informe.id, documentoId).subscribe({
      next: (preview) => this.emlPreviewActivo.set({ documentoId, preview }),
      error: () => this.errorRequeridos.set('No se pudo cargar el preview del EML.')
    });
  }

  cerrarPreviewEml(): void {
    this.emlPreviewActivo.set(null);
  }

  eliminarArchivoRequerido(documentoId: number, claveLogica: string): void {
    const informe = this.informe();
    if (!informe || !documentoId) return;

    this.documentoRequeridoService.eliminarArchivo(informe.id, documentoId).subscribe({
      next: () => {
        this.documentosRequeridos.update((docs) =>
          docs.map((d) => d.claveLogica === claveLogica
            ? { ...d, cargado: false, nombreArchivo: null, contentType: null, extension: null, tamanoBytes: null, id: null }
            : d
          )
        );
      },
      error: () => this.errorRequeridos.set('No se pudo eliminar el archivo.')
    });
  }

  puedeEditarRequeridos(estado: EstadoInforme): boolean {
    return estado === 'BORRADOR' || estado === 'DEVUELTO';
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
