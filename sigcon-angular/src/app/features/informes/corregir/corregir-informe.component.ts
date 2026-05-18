import { SlicePipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, map, of, switchMap } from 'rxjs';

import { ActividadInforme } from '../../../core/models/actividad-informe.model';
import { AporteSgssiRequest, ITEM_SGSSI_LABELS, ItemSgssi } from '../../../core/models/aporte-sgssi.model';
import { DocumentoRequerido, EXTENSIONES_PERMITIDAS_REQUERIDOS } from '../../../core/models/documento-requerido.model';
import { EstadoInforme, InformeDetalle } from '../../../core/models/informe.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { AporteSgssiService } from '../../../core/services/aporte-sgssi.service';
import { DocumentoRequeridoService } from '../../../core/services/documento-requerido.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

interface ActividadFormRow {
  idActividad: number | null;
  idObligacion: number;
  orden: number;
  descripcionObligacion: string;
  descripcion: string;
  soporteUrlId: number | null;
  soporteUrlOriginal: string;
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
  selector: 'app-corregir-informe',
  standalone: true,
  imports: [FormsModule, StatusChipComponent, SlicePipe],
  templateUrl: './corregir-informe.component.html'
})
export class CorregirInformeComponent implements OnInit {
  readonly itemsSgssi: ItemSgssi[] = ITEMS_SGSSI;

  readonly informe = signal<InformeDetalle | null>(null);
  readonly actividadesForm = signal<ActividadFormRow[]>([]);
  readonly error = signal('');
  readonly guardando = signal(false);
  readonly estadoInvalido = signal(false);

  // I8 T3: fecha de elaboración
  readonly fechaElaboracion = signal<string>('');

  // T11: aportes SGSSI editables en DEVUELTO
  readonly aportesEdicion = signal<AporteSgssiEditRow[]>([]);
  readonly guardandoAportes = signal(false);
  readonly errorAportes = signal('');

  // T11: documentos requeridos editables en DEVUELTO
  readonly documentosRequeridos = signal<DocumentoRequerido[]>([]);
  readonly errorRequeridos = signal('');

  readonly observaciones = computed(() => this.informe()?.observaciones ?? []);

  constructor(
    private readonly informeService: InformeService,
    private readonly actividadService: ActividadInformeService,
    private readonly soporteService: SoporteAdjuntoService,
    private readonly aporteSgssiService: AporteSgssiService,
    private readonly documentoRequeridoService: DocumentoRequeridoService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('ID de informe inválido.');
      return;
    }

    this.informeService.obtenerDetalle(id).subscribe({
      next: (informe) => {
        if (!this.esEditable(informe.estado)) {
          this.informe.set(informe);
          this.estadoInvalido.set(true);
          return;
        }
        this.informe.set(informe);
        this.poblarFormulario(informe);
        this.inicializarAportesEdicion(informe);
        this.cargarDocumentosRequeridos(id);
      },
      error: () => this.error.set('No se pudo cargar el informe.')
    });
  }

  guardarBorrador(reenviarDespues = false) {
    const informe = this.informe();
    if (!informe || !this.validarFormulario()) return;

    this.guardando.set(true);
    this.error.set('');

    this.informeService.actualizarInforme(informe.id, {
      idContrato: informe.contratoId ?? 0,
      fechaInicio: informe.fechaInicio,
      fechaFin: informe.fechaFin,
      fechaElaboracion: this.fechaElaboracion() || null,
    }).pipe(
      switchMap((actualizado) => this.guardarDetalle(actualizado)),
      switchMap((actualizado) => this.guardarAportesSgssi(actualizado)),
      switchMap((actualizado) =>
        reenviarDespues ? this.informeService.enviarInforme(actualizado.id) : of(actualizado)
      )
    ).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        void this.router.navigate(['/informes', actualizado.id]);
      },
      error: () => {
        this.guardando.set(false);
        this.error.set('No se pudo guardar las correcciones. Revise los datos e intente nuevamente.');
      }
    });
  }

  confirmarReenvio() {
    if (window.confirm('¿Desea reenviar este informe corregido para revisión?')) {
      this.guardarBorrador(true);
    }
  }

  actualizarActividad(index: number, patch: Partial<ActividadFormRow>) {
    this.actividadesForm.update((rows) => rows.map((row, i) => i === index ? { ...row, ...patch } : row));
  }

  seleccionarArchivo(index: number, event: Event) {
    const input = event.target as HTMLInputElement;
    this.actualizarActividad(index, { soporteArchivo: input.files?.item(0) ?? null });
  }

  // ── Aportes SGSSI (T11) ───────────────────────────────────────────────────

  agregarAporte(): void {
    this.aportesEdicion.update((rows) => [
      ...rows,
      { item: 'SALUD', fechaPago: '', valorAportado: null, entidad: '' }
    ]);
  }

  eliminarAporte(index: number): void {
    this.aportesEdicion.update((rows) => rows.filter((_, i) => i !== index));
  }

  actualizarAporte(index: number, patch: Partial<AporteSgssiEditRow>): void {
    this.aportesEdicion.update((rows) => rows.map((row, i) => i === index ? { ...row, ...patch } : row));
  }

  labelSgssi(item: ItemSgssi): string {
    return ITEM_SGSSI_LABELS[item];
  }

  // ── Documentos requeridos (T11) ───────────────────────────────────────────

  cargarDocumento(claveLogica: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.item(0);
    if (!archivo) return;

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
        input.value = '';
      },
      error: () => {
        this.errorRequeridos.set('No se pudo cargar el archivo. Verifique el formato e intente de nuevo.');
        input.value = '';
      }
    });
  }

  descargarDocumento(documentoId: number, nombreArchivo: string | null): void {
    const informe = this.informe();
    if (!informe) return;
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

  eliminarDocumento(documentoId: number, claveLogica: string): void {
    const informe = this.informe();
    if (!informe) return;
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

  // ── Navegación ────────────────────────────────────────────────────────────

  volverADetalle() {
    const informe = this.informe();
    void this.router.navigate(informe ? ['/informes', informe.id] : ['/contratos']);
  }

  toNumber(value: string | number): number {
    return Number(value) || 0;
  }

  esEditable(estado: EstadoInforme): boolean {
    return estado === 'BORRADOR' || estado === 'DEVUELTO';
  }

  estadoLabel(estado: EstadoInforme): string {
    const map: Record<EstadoInforme, string> = {
      BORRADOR: 'Borrador',
      ENVIADO: 'Enviado',
      EN_REVISION: 'En revisión',
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

  rolLabel(rol: string): string {
    const map: Record<string, string> = {
      REVISOR: 'Revisor',
      SUPERVISOR: 'Supervisor'
    };
    return map[rol] ?? rol;
  }

  // ── Privados ──────────────────────────────────────────────────────────────

  private poblarFormulario(informe: InformeDetalle) {
    this.fechaElaboracion.set(informe.fechaElaboracion ?? new Date().toISOString().slice(0, 10));
    this.actividadesForm.set(
      informe.actividades.map((actividad) => {
        const soporteUrl = actividad.soportes.find((soporte) => soporte.tipo === 'URL');
        return {
          idActividad: actividad.id,
          idObligacion: actividad.idObligacion ?? 0,
          orden: actividad.ordenObligacion ?? 0,
          descripcionObligacion: actividad.descripcionObligacion ?? '',
          descripcion: actividad.descripcion,
          soporteUrlId: soporteUrl?.id ?? null,
          soporteUrlOriginal: soporteUrl?.referencia ?? '',
          soporteNombre: soporteUrl?.nombre ?? '',
          soporteUrl: soporteUrl?.referencia ?? '',
          soporteArchivo: null
        };
      })
    );
  }

  /**
   * T11: Precarga aportes SGSSI desde datos existentes del informe.
   * Si no hay aportes, precarga filas vacías con entidades predeterminadas del contratista.
   */
  private inicializarAportesEdicion(informe: InformeDetalle): void {
    const aportesExistentes = informe.aportesSgssi ?? [];
    if (aportesExistentes.length > 0) {
      this.aportesEdicion.set(aportesExistentes.map((a) => ({
        item: a.item,
        fechaPago: a.fechaPago,
        valorAportado: a.valorAportado,
        entidad: a.entidad
      })));
    } else {
      // Precargar con datos predeterminados del contratista si existen
      const contratista = informe.contratista;
      const rows: AporteSgssiEditRow[] = ITEMS_SGSSI.map((item) => {
        let entidad = '';
        if (contratista) {
          if (item === 'SALUD') entidad = contratista.sgssiSaludEntidad ?? '';
          else if (item === 'PENSION') entidad = contratista.sgssiPensionEntidad ?? '';
          else if (item === 'ARL') entidad = contratista.sgssiArlEntidad ?? '';
        }
        return { item, fechaPago: '', valorAportado: null, entidad };
      });
      this.aportesEdicion.set(rows);
    }
  }

  private cargarDocumentosRequeridos(informeId: number): void {
    this.documentoRequeridoService.listar(informeId).subscribe({
      next: (docs) => this.documentosRequeridos.set(docs),
      error: () => { /* no bloquear la vista */ }
    });
  }

  private guardarDetalle(informe: InformeDetalle) {
    const operaciones = [
      ...this.actividadesForm().map((row) => {
        if (row.idActividad) {
          return this.actividadService.actualizar(informe.id, row.idActividad, {
            idObligacion: row.idObligacion,
            descripcion: row.descripcion.trim(),
          }).pipe(switchMap((actividad) => this.guardarSoportesNuevos(row, actividad)));
        }
        return this.actividadService.crear(informe.id, {
          idObligacion: row.idObligacion,
          descripcion: row.descripcion.trim(),
        }).pipe(switchMap((actividad) => this.guardarSoportesNuevos(row, actividad)));
      })
    ];

    return (operaciones.length ? forkJoin(operaciones) : of([])).pipe(map(() => informe));
  }

  private guardarAportesSgssi(informe: InformeDetalle) {
    const validos: AporteSgssiRequest[] = this.aportesEdicion()
      .filter((row) => row.fechaPago && row.valorAportado != null && row.entidad.trim())
      .map((row) => ({
        item: row.item,
        fechaPago: row.fechaPago,
        valorAportado: row.valorAportado!,
        entidad: row.entidad.trim()
      }));

    if (validos.length === 0) return of(informe);

    return this.aporteSgssiService.guardarTodos(informe.id, validos).pipe(map(() => informe));
  }

  private guardarSoportesNuevos(row: ActividadFormRow, actividad: ActividadInforme) {
    const operaciones = [];
    const soporteNombre = row.soporteNombre.trim() || `Soporte obligación ${row.orden}`;
    const soporteUrl = row.soporteUrl.trim();
    if (soporteUrl && soporteUrl !== row.soporteUrlOriginal) {
      const agregarUrl = this.soporteService.agregarUrl(actividad.id, { nombre: soporteNombre, url: soporteUrl });
      operaciones.push(row.soporteUrlId
        ? this.soporteService.eliminar(actividad.id, row.soporteUrlId).pipe(switchMap(() => agregarUrl))
        : agregarUrl
      );
    }
    if (row.soporteArchivo) {
      operaciones.push(this.soporteService.agregarArchivo(actividad.id, row.soporteArchivo));
    }
    return (operaciones.length ? forkJoin(operaciones) : of([])).pipe(map(() => actividad));
  }

  private validarFormulario() {
    if (this.actividadesForm().some((row) => !row.descripcion.trim())) {
      this.error.set('Debe registrar la actividad realizada para cada obligación.');
      return false;
    }
    return true;
  }
}
