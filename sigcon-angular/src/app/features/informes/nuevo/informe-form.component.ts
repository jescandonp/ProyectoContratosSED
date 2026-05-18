import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, map, of, switchMap } from 'rxjs';

import { ActividadInforme } from '../../../core/models/actividad-informe.model';
import { AporteSgssiRequest, ITEM_SGSSI_LABELS, ItemSgssi } from '../../../core/models/aporte-sgssi.model';
import { ContratoDetalle } from '../../../core/models/contrato.model';
import { InformeDetalle } from '../../../core/models/informe.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { AporteSgssiService } from '../../../core/services/aporte-sgssi.service';
import { ContratoService } from '../../../core/services/contrato.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';

interface ActividadFormRow {
  idObligacion: number;
  orden: number;
  descripcionObligacion: string;
  descripcion: string;
  soporteNombre: string;
  soporteUrl: string;
  soporteArchivo: File | null;
}

interface AporteSgssiRow {
  item: ItemSgssi;
  fechaPago: string;
  valorAportado: number | null;
  entidad: string;
}

const ITEMS_SGSSI: ItemSgssi[] = ['SALUD', 'PENSION', 'ARL'];

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
        <!-- Encabezado del contrato + periodo -->
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

          <div class="mt-lg grid grid-cols-1 gap-md md:grid-cols-2 lg:grid-cols-4">
            <div class="space-y-xs">
              <label class="block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">N.° Desembolso</label>
              <input
                class="w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm py-xs text-sm"
                type="number"
                min="1"
                placeholder="Ej. 3"
                [ngModel]="numeroDesembolso()"
                (ngModelChange)="numeroDesembolso.set($event ? Number($event) : null)"
              />
            </div>
            <div class="space-y-xs">
              <label class="block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Valor Desembolso (COP)</label>
              <input
                class="w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm py-xs text-sm"
                type="number"
                min="0"
                placeholder="Ej. 6000000"
                [ngModel]="valorDesembolso()"
                (ngModelChange)="valorDesembolso.set($event ? Number($event) : null)"
              />
            </div>
            <div class="space-y-xs">
              <label class="block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">% Ejecución acumulada</label>
              <input
                class="w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm py-xs text-sm"
                type="number"
                min="0"
                max="100"
                step="0.01"
                placeholder="Ej. 45.5"
                [ngModel]="porcentajeEjecucion()"
                (ngModelChange)="porcentajeEjecucion.set($event ? Number($event) : null)"
              />
            </div>
            <div class="flex flex-col justify-end space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Correspondencia pendiente</label>
              <label class="flex cursor-pointer items-center gap-sm">
                <input
                  class="h-4 w-4 accent-[var(--color-primary)]"
                  type="checkbox"
                  [ngModel]="correspondenciaPendiente()"
                  (ngModelChange)="correspondenciaPendiente.set($event)"
                />
                <span class="text-sm text-[var(--color-on-surface)]">Sí, hay correspondencia pendiente</span>
              </label>
            </div>
            <!-- Fecha de elaboración -->
            <div class="space-y-xs">
              <label class="block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
                Fecha de elaboración
              </label>
              <input
                class="w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm py-xs text-sm"
                type="date"
                [ngModel]="fechaElaboracion()"
                (ngModelChange)="fechaElaboracion.set($event)"
              />
            </div>
          </div>

          <div class="mt-lg grid grid-cols-1 gap-md md:grid-cols-3">
            <div class="rounded-lg bg-[var(--color-surface-container-low)] p-md">
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Obligaciones</p>
              <p class="m-0 mt-xs text-xl font-bold text-[var(--color-on-surface)]">{{ actividadesForm().length }}</p>
            </div>
            <div class="rounded-lg bg-[var(--color-surface-container-low)] p-md">
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Aportes SGSSI</p>
              <p class="m-0 mt-xs text-xl font-bold text-[var(--color-on-surface)]">{{ aportesForm().length }}</p>
            </div>
          </div>
        </section>

        <!-- Aportes SGSSI -->
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="mb-md flex items-center justify-between">
            <div class="flex items-center gap-sm">
              <span class="h-5 w-1 rounded-full bg-[var(--color-tertiary-container,#e8d5f0)]"></span>
              <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Aportes al Sistema General de Seguridad Social</h3>
            </div>
            <button
              class="flex items-center gap-xs rounded border border-[var(--color-primary)] px-sm py-xs text-xs font-semibold text-[var(--color-primary)] hover:bg-[var(--color-surface-container-low)]"
              type="button"
              (click)="agregarAporte()"
            >
              + Añadir
            </button>
          </div>
          <p class="mb-md text-xs text-[var(--color-on-surface-variant)]">
            Período de aportes: mes inmediatamente anterior al inicio del período del informe.
          </p>
          @if (aportesForm().length === 0) {
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin aportes registrados. Agregue los conceptos SALUD, PENSIÓN y A.R.L.</p>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-[var(--color-outline-variant)] text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
                    <th class="pb-xs pr-sm">Concepto</th>
                    <th class="pb-xs pr-sm">Entidad</th>
                    <th class="pb-xs pr-sm">Fecha de pago</th>
                    <th class="pb-xs pr-sm">Valor aportado (COP)</th>
                    <th class="pb-xs"></th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[var(--color-outline-variant)]">
                  @for (row of aportesForm(); track $index; let i = $index) {
                    <tr>
                      <td class="py-xs pr-sm">
                        <select
                          class="w-full rounded border border-[var(--color-outline-variant)] px-xs py-xs text-sm"
                          [ngModel]="row.item"
                          (ngModelChange)="actualizarAporte(i, { item: $event })"
                        >
                          @for (it of itemsSgssi; track it) {
                            <option [value]="it">{{ labelSgssi(it) }}</option>
                          }
                        </select>
                      </td>
                      <td class="py-xs pr-sm">
                        <input
                          class="w-full rounded border border-[var(--color-outline-variant)] px-xs py-xs text-sm"
                          type="text"
                          maxlength="200"
                          placeholder="Nombre entidad"
                          [ngModel]="row.entidad"
                          (ngModelChange)="actualizarAporte(i, { entidad: $event })"
                        />
                      </td>
                      <td class="py-xs pr-sm">
                        <input
                          class="w-full rounded border border-[var(--color-outline-variant)] px-xs py-xs text-sm"
                          type="date"
                          [ngModel]="row.fechaPago"
                          (ngModelChange)="actualizarAporte(i, { fechaPago: $event })"
                        />
                      </td>
                      <td class="py-xs pr-sm">
                        <input
                          class="w-full rounded border border-[var(--color-outline-variant)] px-xs py-xs text-sm"
                          type="number"
                          min="0"
                          [ngModel]="row.valorAportado"
                          (ngModelChange)="actualizarAporte(i, { valorAportado: $event ? Number($event) : null })"
                        />
                      </td>
                      <td class="py-xs">
                        <button
                          class="text-[var(--color-error)] hover:opacity-70"
                          type="button"
                          (click)="eliminarAporte(i)"
                          title="Eliminar"
                        >
                          <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path d="M18 6L6 18M6 6l12 12"/>
                          </svg>
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <!-- Actividades por obligación -->
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

                <label class="block">
                  <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Actividad realizada</span>
                  <textarea
                    class="min-h-28 w-full resize-y rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                    [ngModel]="row.descripcion"
                    (ngModelChange)="actualizarActividad(i, { descripcion: $event })"
                  ></textarea>
                </label>

                <div class="mt-md grid grid-cols-1 gap-md lg:grid-cols-2">
                  <label class="block">
                    <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Nombre soporte *</span>
                    <input
                      class="w-full rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                      type="text"
                      [ngModel]="row.soporteNombre"
                      (ngModelChange)="actualizarActividad(i, { soporteNombre: $event })"
                    />
                  </label>
                  <label class="block">
                    <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">URL soporte *</span>
                    <input
                      class="w-full rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
                      type="url"
                      [ngModel]="row.soporteUrl"
                      (ngModelChange)="actualizarActividad(i, { soporteUrl: $event })"
                    />
                  </label>
                </div>
              </article>
            }
          </div>
        </section>

        <!-- Barra fija de acciones -->
        <div class="fixed inset-x-0 bottom-0 z-10 border-t border-[var(--color-outline-variant)] bg-white/95 px-lg py-md backdrop-blur">
          <div class="mx-auto flex max-w-6xl items-center justify-between gap-md">
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">
              {{ guardando() ? 'Guardando informe...' : 'Revise actividades y aportes SGSSI antes de enviar.' }}
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
  readonly itemsSgssi: ItemSgssi[] = ITEMS_SGSSI;

  readonly contrato = signal<ContratoDetalle | null>(null);
  readonly fechaInicio = signal('');
  readonly fechaFin = signal('');
  readonly actividadesForm = signal<ActividadFormRow[]>([]);
  readonly aportesForm = signal<AporteSgssiRow[]>([]);
  readonly error = signal('');
  readonly guardando = signal(false);

  // I6 header fields
  readonly numeroDesembolso   = signal<number | null>(null);
  readonly valorDesembolso    = signal<number | null>(null);
  readonly porcentajeEjecucion = signal<number | null>(null);
  readonly correspondenciaPendiente = signal<boolean>(false);
  // I8 T3: fecha de elaboración
  readonly fechaElaboracion = signal<string>(new Date().toISOString().slice(0, 10));

  constructor(
    private readonly contratoService: ContratoService,
    private readonly informeService: InformeService,
    private readonly actividadService: ActividadInformeService,
    private readonly soporteService: SoporteAdjuntoService,
    private readonly aporteSgssiService: AporteSgssiService,
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
              soporteNombre: '',
              soporteUrl: '',
              soporteArchivo: null
            }))
        );
      },
      error: () => this.error.set('No se pudo cargar el contrato para crear el informe.')
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
      fechaFin: this.fechaFin(),
      numeroDesembolso: this.numeroDesembolso(),
      valorDesembolso: this.valorDesembolso(),
      porcentajeEjecucion: this.porcentajeEjecucion(),
      correspondenciaPendiente: this.correspondenciaPendiente(),
      fechaElaboracion: this.fechaElaboracion() || null,
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

  seleccionarArchivo(index: number, event: Event) {
    const input = event.target as HTMLInputElement;
    this.actualizarActividad(index, { soporteArchivo: input.files?.item(0) ?? null });
  }

  agregarAporte() {
    this.aportesForm.update((rows) => [...rows, { item: 'SALUD', fechaPago: '', valorAportado: null, entidad: '' }]);
  }

  eliminarAporte(index: number) {
    this.aportesForm.update((rows) => rows.filter((_, i) => i !== index));
  }

  actualizarAporte(index: number, patch: Partial<AporteSgssiRow>) {
    this.aportesForm.update((rows) => rows.map((row, i) => i === index ? { ...row, ...patch } : row));
  }

  labelSgssi(item: ItemSgssi): string {
    return ITEM_SGSSI_LABELS[item];
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  Number(value: any): number { return Number(value) || 0; }


  volverAContrato() {
    const contrato = this.contrato();
    void this.router.navigate(contrato ? ['/contratos', contrato.id] : ['/contratos']);
  }

  private guardarDetalle(informe: InformeDetalle) {
    const actividadOps = this.actividadesForm().map((row) =>
      this.actividadService.crear(informe.id, {
        idObligacion: row.idObligacion,
        descripcion: row.descripcion.trim(),
      }).pipe(switchMap((actividad) => this.guardarSoportes(row, actividad)))
    );

    const aportesValidos: AporteSgssiRequest[] = this.aportesForm()
      .filter((row) => row.fechaPago && row.valorAportado != null && row.entidad.trim())
      .map((row) => ({
        item: row.item,
        fechaPago: row.fechaPago,
        valorAportado: row.valorAportado!,
        entidad: row.entidad.trim(),
      }));
    const aportesOp = aportesValidos.length > 0
      ? [this.aporteSgssiService.guardarTodos(informe.id, aportesValidos)]
      : [];

    const todas = [...actividadOps, ...aportesOp];
    return (todas.length ? forkJoin(todas) : of([])).pipe(map(() => informe));
  }

  private guardarSoportes(row: ActividadFormRow, actividad: ActividadInforme) {
    const operaciones = [];
    if (row.soporteUrl.trim()) {
      operaciones.push(this.soporteService.agregarUrl(actividad.id, {
        nombre: row.soporteNombre.trim() || 'Soporte',
        url: row.soporteUrl.trim()
      }));
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
    return true;
  }
}
