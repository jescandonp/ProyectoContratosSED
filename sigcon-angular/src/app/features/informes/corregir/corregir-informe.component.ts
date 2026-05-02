import { SlicePipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, map, of, switchMap } from 'rxjs';

import { ActividadInforme } from '../../../core/models/actividad-informe.model';
import { DocumentoCatalogo } from '../../../core/models/documento-catalogo.model';
import { EstadoInforme, InformeDetalle } from '../../../core/models/informe.model';
import { Observacion } from '../../../core/models/observacion.model';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { InformeService } from '../../../core/services/informe.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

interface ActividadFormRow {
  idActividad: number | null;
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
  idDocumento: number | null;
  idCatalogo: number;
  nombreCatalogo: string;
  obligatorio: boolean;
  referencia: string;
}

@Component({
  selector: 'app-corregir-informe',
  standalone: true,
  imports: [FormsModule, StatusChipComponent, SlicePipe],
  template: `
    <div class="space-y-lg pb-24">
      <nav class="flex items-center gap-xs text-sm text-[var(--color-on-surface-variant)]">
        <button class="text-[var(--color-primary)] hover:underline" type="button" (click)="volverADetalle()">Informes</button>
        <span>/</span>
        <span>{{ informe()?.contratoNumero ?? 'Corrección' }}</span>
        <span>/</span>
        <span>Corrección</span>
      </nav>

      @if (error()) {
        <div class="rounded-xl border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      @if (estadoInvalido()) {
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Este informe no puede ser corregido en su estado actual.
          <button class="ml-sm text-[var(--color-primary)] hover:underline" type="button" (click)="volverADetalle()">Volver al detalle</button>
        </div>
      }

      @if (informe(); as i) {
        @if (!estadoInvalido()) {
          <!-- Cabecera del informe -->
          <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
            <div class="flex flex-col gap-md lg:flex-row lg:items-start lg:justify-between">
              <div>
                <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">Corrección de informe</p>
                <h2 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">{{ i.contratoNumero }} — Informe No. {{ i.numero }}</h2>
                <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">{{ i.fechaInicio }} a {{ i.fechaFin }}</p>
              </div>
              <div class="flex items-center gap-sm">
                <app-status-chip [value]="i.estado" [label]="estadoLabel(i.estado)" [tone]="estadoTone(i.estado)" />
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

          <div class="grid grid-cols-1 gap-lg lg:grid-cols-3">
            <!-- Columna principal: formulario de corrección -->
            <div class="space-y-lg lg:col-span-2">
              <!-- Actividades -->
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

              <!-- Documentos adicionales -->
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
            </div>

            <!-- Panel lateral: observaciones históricas -->
            <aside class="space-y-lg">
              <div class="rounded-xl border border-[var(--color-error-container)] bg-white p-lg">
                <div class="mb-md flex items-center gap-sm">
                  <span class="h-5 w-1 rounded-full bg-[var(--color-error)]"></span>
                  <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Observaciones recibidas</h3>
                </div>

                @if (observaciones().length === 0) {
                  <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin observaciones registradas.</p>
                } @else {
                  <ul class="m-0 list-none space-y-sm p-0">
                    @for (obs of observaciones(); track obs.id) {
                      <li class="rounded-lg border border-[var(--color-error-container)] bg-[var(--color-error-container)] p-sm text-sm">
                        <div class="flex items-center justify-between gap-xs">
                          <span class="font-bold text-[var(--color-on-error-container)]">{{ rolLabel(obs.autorRol) }}</span>
                          @if (obs.fecha) {
                            <span class="text-xs text-[var(--color-on-surface-variant)]">{{ obs.fecha | slice:0:10 }}</span>
                          }
                        </div>
                        <p class="m-0 mt-xs text-[var(--color-on-error-container)]">{{ obs.texto }}</p>
                      </li>
                    }
                  </ul>
                }
              </div>

              <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
                <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Actores</h3>
                <dl class="space-y-md text-sm">
                  <div>
                    <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Supervisor</dt>
                    <dd class="m-0 mt-xs text-[var(--color-on-surface)]">{{ informe()?.supervisor?.nombre ?? 'No asignado' }}</dd>
                  </div>
                  <div>
                    <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Revisor</dt>
                    <dd class="m-0 mt-xs text-[var(--color-on-surface)]">{{ informe()?.revisor?.nombre ?? 'No asignado' }}</dd>
                  </div>
                </dl>
              </div>
            </aside>
          </div>
        }
      } @else if (!error() && !estadoInvalido()) {
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Cargando informe...
        </div>
      }

      <!-- Barra de acciones fija -->
      @if (informe() && !estadoInvalido()) {
        <div class="fixed inset-x-0 bottom-0 z-10 border-t border-[var(--color-outline-variant)] bg-white/95 px-lg py-md backdrop-blur">
          <div class="mx-auto flex max-w-6xl items-center justify-between gap-md">
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">
              {{ guardando() ? 'Guardando correcciones...' : 'Revise las observaciones y corrija las actividades antes de reenviar.' }}
            </p>
            <div class="flex gap-sm">
              <button
                class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]"
                type="button"
                (click)="volverADetalle()"
              >
                Cancelar
              </button>
              <button
                class="rounded border border-[var(--color-primary)] px-md py-sm text-sm font-semibold text-[var(--color-primary)] disabled:opacity-50"
                type="button"
                [disabled]="guardando()"
                (click)="guardarBorrador()"
              >
                Guardar borrador
              </button>
              <button
                class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50"
                type="button"
                [disabled]="guardando()"
                (click)="confirmarReenvio()"
              >
                Reenviar informe
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class CorregirInformeComponent implements OnInit {
  readonly informe = signal<InformeDetalle | null>(null);
  readonly actividadesForm = signal<ActividadFormRow[]>([]);
  readonly documentosForm = signal<DocumentoFormRow[]>([]);
  readonly error = signal('');
  readonly guardando = signal(false);
  readonly estadoInvalido = signal(false);

  readonly observaciones = computed(() => this.informe()?.observaciones ?? []);

  readonly avancePromedio = computed(() => {
    const rows = this.actividadesForm();
    if (rows.length === 0) return 0;
    const total = rows.reduce((sum, row) => sum + Number(row.porcentaje || 0), 0);
    return Math.round(total / rows.length);
  });

  readonly documentosCompletos = computed(() => this.documentosForm().filter((doc) => doc.referencia.trim()).length);

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
        this.cargarCatalogo(informe);
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
      fechaFin: informe.fechaFin
    }).pipe(
      switchMap((actualizado) => this.guardarDetalle(actualizado)),
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

  actualizarDocumento(index: number, referencia: string) {
    this.documentosForm.update((docs) => docs.map((doc, i) => i === index ? { ...doc, referencia } : doc));
  }

  seleccionarArchivo(index: number, event: Event) {
    const input = event.target as HTMLInputElement;
    this.actualizarActividad(index, { soporteArchivo: input.files?.item(0) ?? null });
  }

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

  private poblarFormulario(informe: InformeDetalle) {
    this.actividadesForm.set(
      informe.actividades.map((actividad) => ({
        idActividad: actividad.id,
        idObligacion: actividad.idObligacion ?? 0,
        orden: actividad.ordenObligacion ?? 0,
        descripcionObligacion: actividad.descripcionObligacion ?? '',
        descripcion: actividad.descripcion,
        porcentaje: actividad.porcentaje,
        soporteNombre: '',
        soporteUrl: '',
        soporteArchivo: null
      }))
    );
  }

  private cargarCatalogo(informe: InformeDetalle) {
    this.documentoCatalogoService.listar({ tipoContrato: 'OPS', size: 100 }).subscribe({
      next: (page) => {
        this.documentosForm.set(
          page.content.map((doc) => {
            const existente = informe.documentosAdicionales.find((d) => d.idCatalogo === doc.id);
            return {
              idDocumento: existente?.id ?? null,
              idCatalogo: doc.id,
              nombreCatalogo: doc.nombre,
              obligatorio: doc.obligatorio,
              referencia: existente?.referencia ?? ''
            };
          })
        );
      },
      error: () => this.error.set('No se pudo cargar el catálogo documental OPS.')
    });
  }

  private guardarDetalle(informe: InformeDetalle) {
    const operaciones = [
      ...this.actividadesForm().map((row) => {
        if (row.idActividad) {
          return this.actividadService.actualizar(informe.id, row.idActividad, {
            idObligacion: row.idObligacion,
            descripcion: row.descripcion.trim(),
            porcentaje: Number(row.porcentaje)
          }).pipe(switchMap((actividad) => this.guardarSoportesNuevos(row, actividad)));
        }
        return this.actividadService.crear(informe.id, {
          idObligacion: row.idObligacion,
          descripcion: row.descripcion.trim(),
          porcentaje: Number(row.porcentaje)
        }).pipe(switchMap((actividad) => this.guardarSoportesNuevos(row, actividad)));
      }),
      ...this.documentosForm()
        .filter((doc) => doc.referencia.trim() && !doc.idDocumento)
        .map((doc) =>
          this.documentoAdicionalService.agregar(informe.id, {
            idCatalogo: doc.idCatalogo,
            referencia: doc.referencia.trim()
          })
        )
    ];

    return (operaciones.length ? forkJoin(operaciones) : of([])).pipe(map(() => informe));
  }

  private guardarSoportesNuevos(row: ActividadFormRow, actividad: ActividadInforme) {
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
}
