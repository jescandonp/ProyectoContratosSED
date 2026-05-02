import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, map, of, switchMap } from 'rxjs';

import { ActividadInforme } from '../../../core/models/actividad-informe.model';
import { ContratoDetalle } from '../../../core/models/contrato.model';
import { DocumentoCatalogo } from '../../../core/models/documento-catalogo.model';
import { InformeDetalle } from '../../../core/models/informe.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { ContratoService } from '../../../core/services/contrato.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';

interface ActividadFormRow {
  idObligacion: number;
  orden: number;
  descripcionObligacion: string;
  descripcion: string;
  porcentaje: number;
  soporteNombre: string;
  soporteUrl: string;
  soporteArchivo: File | null;
}

interface DocumentoFormRow {
  idCatalogo: number;
  nombreCatalogo: string;
  obligatorio: boolean;
  referencia: string;
}

@Component({
  selector: 'app-informe-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="space-y-lg pb-24">
      <nav class="flex items-center gap-xs text-sm text-[var(--color-on-surface-variant)]">
        <button class="text-[var(--color-primary)] hover:underline" type="button" (click)="volverAContrato()">Contratos</button>
        <span>/</span>
        <span>{{ contrato()?.numero ?? 'Nuevo informe' }}</span>
      </nav>

      @if (error()) {
        <div class="rounded-xl border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      @if (contrato(); as c) {
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="flex flex-col gap-md lg:flex-row lg:items-start lg:justify-between">
            <div>
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">Informe mensual de actividades</p>
              <h2 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">{{ c.numero }}</h2>
              <p class="mt-xs max-w-3xl text-sm text-[var(--color-on-surface-variant)]">{{ c.objeto }}</p>
            </div>
            <div class="grid min-w-72 grid-cols-2 gap-sm text-sm">
              <label class="block">
                <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Inicio</span>
                <input
                  class="w-full rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                  type="date"
                  [ngModel]="fechaInicio()"
                  (ngModelChange)="fechaInicio.set($event)"
                />
              </label>
              <label class="block">
                <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fin</span>
                <input
                  class="w-full rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                  type="date"
                  [ngModel]="fechaFin()"
                  (ngModelChange)="fechaFin.set($event)"
                />
              </label>
            </div>
          </div>

          <div class="mt-lg grid grid-cols-1 gap-md md:grid-cols-3">
            <div class="rounded-lg bg-[var(--color-surface-container-low)] p-md">
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Obligaciones</p>
              <p class="m-0 mt-xs text-xl font-bold text-[var(--color-on-surface)]">{{ actividadesForm().length }}</p>
            </div>
            <div class="rounded-lg bg-[var(--color-surface-container-low)] p-md">
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Avance declarado</p>
              <p class="m-0 mt-xs text-xl font-bold text-[var(--color-on-surface)]">{{ avancePromedio() }}%</p>
            </div>
            <div class="rounded-lg bg-[var(--color-surface-container-low)] p-md">
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Documentos</p>
              <p class="m-0 mt-xs text-xl font-bold text-[var(--color-on-surface)]">{{ documentosCompletos() }}/{{ documentosForm().length }}</p>
            </div>
          </div>
        </section>

        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="mb-md flex items-center gap-sm">
            <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
            <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Actividades por obligación</h3>
          </div>

          <div class="space-y-md">
            @for (row of actividadesForm(); track row.idObligacion; let i = $index) {
              <article class="rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-md">
                <div class="mb-sm flex items-start gap-sm">
                  <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-bold text-white">
                    {{ row.orden }}
                  </span>
                  <div>
                    <p class="m-0 text-sm font-semibold text-[var(--color-on-surface)]">Obligación {{ row.orden }}</p>
                    <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">{{ row.descripcionObligacion }}</p>
                  </div>
                </div>

                <div class="grid grid-cols-1 gap-md lg:grid-cols-[1fr_10rem]">
                  <label class="block">
                    <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Actividad realizada</span>
                    <textarea
                      class="min-h-28 w-full resize-y rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                      [ngModel]="row.descripcion"
                      (ngModelChange)="actualizarActividad(i, { descripcion: $event })"
                    ></textarea>
                  </label>
                  <label class="block">
                    <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Avance %</span>
                    <input
                      class="w-full rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                      type="number"
                      min="0"
                      max="100"
                      [ngModel]="row.porcentaje"
                      (ngModelChange)="actualizarActividad(i, { porcentaje: toNumber($event) })"
                    />
                  </label>
                </div>

                <div class="mt-md grid grid-cols-1 gap-md lg:grid-cols-3">
                  <label class="block">
                    <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Nombre soporte</span>
                    <input
                      class="w-full rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                      type="text"
                      [ngModel]="row.soporteNombre"
                      (ngModelChange)="actualizarActividad(i, { soporteNombre: $event })"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">URL soporte</span>
                    <input
                      class="w-full rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                      type="url"
                      [ngModel]="row.soporteUrl"
                      (ngModelChange)="actualizarActividad(i, { soporteUrl: $event })"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Archivo</span>
                    <input
                      class="w-full rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-sm"
                      type="file"
                      (change)="seleccionarArchivo(i, $event)"
                    />
                  </label>
                </div>
              </article>
            }
          </div>
        </section>

        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="mb-md flex items-center gap-sm">
            <span class="h-5 w-1 rounded-full bg-[var(--color-secondary-container)]"></span>
            <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Documentos adicionales</h3>
          </div>

          @if (documentosForm().length === 0) {
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">No hay documentos OPS configurados en catálogo.</p>
          } @else {
            <div class="space-y-sm">
              @for (doc of documentosForm(); track doc.idCatalogo; let i = $index) {
                <label class="grid grid-cols-1 gap-sm rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-md md:grid-cols-[1fr_18rem] md:items-center">
                  <span>
                    <span class="block text-sm font-semibold text-[var(--color-on-surface)]">{{ doc.nombreCatalogo }}</span>
                    @if (doc.obligatorio) {
                      <span class="text-xs font-bold uppercase tracking-wider text-[var(--color-error)]">Obligatorio</span>
                    }
                  </span>
                  <input
                    class="w-full rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-sm"
                    type="text"
                    placeholder="Referencia o enlace"
                    [ngModel]="doc.referencia"
                    (ngModelChange)="actualizarDocumento(i, $event)"
                  />
                </label>
              }
            </div>
          }
        </section>

        <div class="fixed inset-x-0 bottom-0 z-10 border-t border-[var(--color-outline-variant)] bg-white/95 px-lg py-md backdrop-blur">
          <div class="mx-auto flex max-w-6xl items-center justify-between gap-md">
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">
              {{ guardando() ? 'Guardando informe...' : 'Revise actividades, soportes y documentos antes de enviar.' }}
            </p>
            <div class="flex gap-sm">
              <button class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]" type="button" (click)="volverAContrato()">
                Cancelar
              </button>
              <button class="rounded border border-[var(--color-primary)] px-md py-sm text-sm font-semibold text-[var(--color-primary)] disabled:opacity-50" type="button" [disabled]="guardando()" (click)="guardarBorrador()">
                Guardar borrador
              </button>
              <button class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50" type="button" [disabled]="guardando()" (click)="confirmarEnvio()">
                Enviar informe
              </button>
            </div>
          </div>
        </div>
      } @else if (!error()) {
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Cargando contrato...
        </div>
      }
    </div>
  `
})
export class InformeFormComponent implements OnInit {
  readonly contrato = signal<ContratoDetalle | null>(null);
  readonly fechaInicio = signal('');
  readonly fechaFin = signal('');
  readonly actividadesForm = signal<ActividadFormRow[]>([]);
  readonly documentosForm = signal<DocumentoFormRow[]>([]);
  readonly error = signal('');
  readonly guardando = signal(false);

  readonly avancePromedio = computed(() => {
    const rows = this.actividadesForm();
    if (rows.length === 0) return 0;
    const total = rows.reduce((sum, row) => sum + Number(row.porcentaje || 0), 0);
    return Math.round(total / rows.length);
  });

  readonly documentosCompletos = computed(() => this.documentosForm().filter((doc) => doc.referencia.trim()).length);

  constructor(
    private readonly contratoService: ContratoService,
    private readonly documentoCatalogoService: DocumentoCatalogoService,
    private readonly informeService: InformeService,
    private readonly actividadService: ActividadInformeService,
    private readonly soporteService: SoporteAdjuntoService,
    private readonly documentoAdicionalService: DocumentoAdicionalService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    const contratoId = Number(this.route.snapshot.paramMap.get('contratoId'));
    if (!contratoId) {
      this.error.set('ID de contrato inválido.');
      return;
    }

    this.contratoService.obtenerDetalle(contratoId).subscribe({
      next: (contrato) => {
        this.contrato.set(contrato);
        this.fechaInicio.set(contrato.fechaInicio);
        this.fechaFin.set(contrato.fechaFin);
        this.actividadesForm.set(
          [...contrato.obligaciones]
            .sort((a, b) => a.orden - b.orden)
            .map((obligacion) => ({
              idObligacion: obligacion.id,
              orden: obligacion.orden,
              descripcionObligacion: obligacion.descripcion,
              descripcion: '',
              porcentaje: 0,
              soporteNombre: '',
              soporteUrl: '',
              soporteArchivo: null
            }))
        );
      },
      error: () => this.error.set('No se pudo cargar el contrato para crear el informe.')
    });

    this.documentoCatalogoService.listar({ tipoContrato: 'OPS', size: 100 }).subscribe({
      next: (page) => this.documentosForm.set(page.content.map((doc) => this.toDocumentoForm(doc))),
      error: () => this.error.set('No se pudo cargar el catálogo documental OPS.')
    });
  }

  guardarBorrador(enviarDespues = false) {
    const contrato = this.contrato();
    if (!contrato || !this.validarFormulario()) return;

    this.guardando.set(true);
    this.error.set('');

    this.informeService.crearInforme({
      idContrato: contrato.id,
      fechaInicio: this.fechaInicio(),
      fechaFin: this.fechaFin()
    }).pipe(
      switchMap((informe) => this.guardarDetalle(informe)),
      switchMap((informe) => enviarDespues ? this.informeService.enviarInforme(informe.id) : of(informe))
    ).subscribe({
      next: (informe) => {
        this.guardando.set(false);
        void this.router.navigate(['/informes', informe.id]);
      },
      error: () => {
        this.guardando.set(false);
        this.error.set('No se pudo guardar el informe. Revise los datos e intente nuevamente.');
      }
    });
  }

  confirmarEnvio() {
    if (window.confirm('¿Desea enviar este informe para revisión?')) {
      this.guardarBorrador(true);
    }
  }

  actualizarActividad(index: number, patch: Partial<ActividadFormRow>) {
    this.actividadesForm.update((rows) => rows.map((row, i) => i === index ? { ...row, ...patch } : row));
  }

  actualizarDocumento(index: number, referencia: string) {
    this.documentosForm.update((docs) => docs.map((doc, i) => i === index ? { ...doc, referencia } : doc));
  }

  seleccionarArchivo(index: number, event: Event) {
    const input = event.target as HTMLInputElement;
    this.actualizarActividad(index, { soporteArchivo: input.files?.item(0) ?? null });
  }

  volverAContrato() {
    const contrato = this.contrato();
    void this.router.navigate(contrato ? ['/contratos', contrato.id] : ['/contratos']);
  }

  toNumber(value: string | number): number {
    return Number(value) || 0;
  }

  private guardarDetalle(informe: InformeDetalle) {
    const operaciones = [
      ...this.actividadesForm().map((row) =>
        this.actividadService.crear(informe.id, {
          idObligacion: row.idObligacion,
          descripcion: row.descripcion.trim(),
          porcentaje: Number(row.porcentaje)
        }).pipe(switchMap((actividad) => this.guardarSoportes(row, actividad)))
      ),
      ...this.documentosForm()
        .filter((doc) => doc.referencia.trim())
        .map((doc) => this.documentoAdicionalService.agregar(informe.id, {
          idCatalogo: doc.idCatalogo,
          referencia: doc.referencia.trim()
        }))
    ];

    return (operaciones.length ? forkJoin(operaciones) : of([])).pipe(map(() => informe));
  }

  private guardarSoportes(row: ActividadFormRow, actividad: ActividadInforme) {
    const operaciones = [];
    const soporteNombre = row.soporteNombre.trim() || `Soporte obligación ${row.orden}`;
    if (row.soporteUrl.trim()) {
      operaciones.push(this.soporteService.agregarUrl(actividad.id, { nombre: soporteNombre, url: row.soporteUrl.trim() }));
    }
    if (row.soporteArchivo) {
      operaciones.push(this.soporteService.agregarArchivo(actividad.id, row.soporteArchivo));
    }

    return (operaciones.length ? forkJoin(operaciones) : of([])).pipe(map(() => actividad));
  }

  private validarFormulario() {
    if (!this.fechaInicio() || !this.fechaFin()) {
      this.error.set('Debe indicar el periodo del informe.');
      return false;
    }
    if (this.actividadesForm().some((row) => !row.descripcion.trim())) {
      this.error.set('Debe registrar la actividad realizada para cada obligación.');
      return false;
    }
    if (this.actividadesForm().some((row) => row.porcentaje < 0 || row.porcentaje > 100)) {
      this.error.set('El porcentaje de avance debe estar entre 0 y 100.');
      return false;
    }
    if (this.documentosForm().some((doc) => doc.obligatorio && !doc.referencia.trim())) {
      this.error.set('Debe registrar la referencia de los documentos obligatorios.');
      return false;
    }
    return true;
  }

  private toDocumentoForm(doc: DocumentoCatalogo): DocumentoFormRow {
    return {
      idCatalogo: doc.id,
      nombreCatalogo: doc.nombre,
      obligatorio: doc.obligatorio,
      referencia: ''
    };
  }
}
