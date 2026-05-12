import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  BusquedaAdminFiltros,
  BusquedaAdminPageResponse,
  BusquedaAdminResponse,
  BusquedaAdminService,
  ContratistaResultado,
  ContratoResultado,
  InformeResultado
} from '../../../core/services/busqueda-admin.service';

const ESTADOS_CONTRATO = [
  { value: '', label: 'Todos' },
  { value: 'EN_EJECUCION', label: 'En ejecución' },
  { value: 'LIQUIDADO', label: 'Liquidado' },
  { value: 'CERRADO', label: 'Cerrado' }
];

const ESTADOS_INFORME = [
  { value: '', label: 'Todos' },
  { value: 'BORRADOR', label: 'Borrador' },
  { value: 'ENVIADO', label: 'Enviado' },
  { value: 'EN_REVISION', label: 'En revisión' },
  { value: 'DEVUELTO', label: 'Devuelto' },
  { value: 'APROBADO', label: 'Aprobado' }
];

@Component({
  selector: 'app-admin-busqueda',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="space-y-lg">
      <div>
        <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Búsqueda global</h2>
        <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">
          Busca contratos e informes con filtros combinados. El texto libre es opcional.
        </p>
      </div>

      <!-- Formulario de búsqueda avanzada -->
      <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
        <div class="grid grid-cols-1 gap-md sm:grid-cols-2 lg:grid-cols-3">
          <!-- Texto libre -->
          <label class="block sm:col-span-2 lg:col-span-3">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Texto libre (opcional)
            </span>
            <input
              class="h-9 w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none focus:border-[var(--color-primary)]"
              type="text"
              placeholder="Nombre, número de contrato, estado..."
              data-testid="input-busqueda"
              [(ngModel)]="termino"
              (keyup.enter)="buscar()"
            />
          </label>

          <!-- Estado contrato -->
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Estado contrato
            </span>
            <select
              class="h-9 w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none focus:border-[var(--color-primary)]"
              data-testid="select-estado-contrato"
              [(ngModel)]="estadoContrato"
            >
              @for (e of estadosContrato; track e.value) {
                <option [value]="e.value">{{ e.label }}</option>
              }
            </select>
          </label>

          <!-- Estado informe -->
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Estado informe
            </span>
            <select
              class="h-9 w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none focus:border-[var(--color-primary)]"
              data-testid="select-estado-informe"
              [(ngModel)]="estadoInforme"
            >
              @for (e of estadosInforme; track e.value) {
                <option [value]="e.value">{{ e.label }}</option>
              }
            </select>
          </label>

          <!-- Periodo desde -->
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Periodo desde
            </span>
            <input
              class="h-9 w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none focus:border-[var(--color-primary)]"
              type="date"
              data-testid="input-fecha-inicio"
              [(ngModel)]="fechaInicio"
            />
          </label>

          <!-- Periodo hasta -->
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Periodo hasta
            </span>
            <input
              class="h-9 w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none focus:border-[var(--color-primary)]"
              type="date"
              data-testid="input-fecha-fin"
              [(ngModel)]="fechaFin"
            />
          </label>

          <!-- Botón buscar -->
          <div class="flex items-end sm:col-span-2 lg:col-span-1">
            <button
              class="h-9 w-full rounded bg-[var(--color-primary)] px-lg text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
              type="button"
              data-testid="btn-buscar"
              [disabled]="buscando()"
              (click)="buscar()"
            >
              {{ buscando() ? 'Buscando...' : 'Buscar' }}
            </button>
          </div>
        </div>

        @if (error()) {
          <p class="mt-sm text-xs text-[var(--color-error)]" data-testid="error-busqueda">{{ error() }}</p>
        }
      </section>

      <!-- Resultados avanzados: contratos con informes anidados -->
      @if (resultadosAvanzados()) {
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg" data-testid="seccion-contratos-avanzada">
          <div class="mb-md flex items-center justify-between gap-sm">
            <div class="flex items-center gap-sm">
              <span class="h-5 w-1 rounded-full bg-[var(--color-secondary-container)]"></span>
              <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">
                Contratos
                <span class="ml-sm text-xs font-normal text-[var(--color-on-surface-variant)]">
                  ({{ resultadosAvanzados()!.totalElementos }} total)
                </span>
              </h3>
            </div>
            <!-- Paginación -->
            @if (resultadosAvanzados()!.totalPaginas > 1) {
              <div class="flex items-center gap-xs text-sm" data-testid="paginacion">
                <button
                  class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-xs disabled:opacity-40"
                  type="button"
                  data-testid="btn-pagina-anterior"
                  [disabled]="paginaActual() === 0"
                  (click)="irAPagina(paginaActual() - 1)"
                >‹ Anterior</button>
                <span class="text-xs text-[var(--color-on-surface-variant)]">
                  Pág. {{ paginaActual() + 1 }} / {{ resultadosAvanzados()!.totalPaginas }}
                </span>
                <button
                  class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-xs disabled:opacity-40"
                  type="button"
                  data-testid="btn-pagina-siguiente"
                  [disabled]="paginaActual() >= resultadosAvanzados()!.totalPaginas - 1"
                  (click)="irAPagina(paginaActual() + 1)"
                >Siguiente ›</button>
              </div>
            }
          </div>

          @if (resultadosAvanzados()!.contratos.length === 0) {
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin resultados para los filtros aplicados.</p>
          } @else {
            <div class="space-y-md">
              @for (c of resultadosAvanzados()!.contratos; track c.id) {
                <article class="rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-md"
                         [attr.data-testid]="'resultado-contrato-' + c.id">
                  <!-- Cabecera del contrato -->
                  <div class="flex items-start justify-between gap-md">
                    <div>
                      <span class="font-semibold text-[var(--color-on-surface)]">{{ c.numero }}</span>
                      <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">{{ c.estado }}</span>
                      @if (c.contratistaNombre) {
                        <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· {{ c.contratistaNombre }}</span>
                      }
                      <p class="m-0 mt-xs text-xs text-[var(--color-on-surface-variant)]">{{ c.objeto }}</p>
                    </div>
                    <button
                      class="flex-shrink-0 text-xs font-semibold text-[var(--color-primary)] hover:underline"
                      type="button"
                      (click)="irAContrato(c.id)"
                    >Ver contrato</button>
                  </div>

                  <!-- Informes anidados -->
                  @if (c.informes && c.informes.length > 0) {
                    <div class="mt-sm border-t border-[var(--color-outline-variant)] pt-sm">
                      <p class="mb-xs text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
                        Informes ({{ c.informes.length }})
                      </p>
                      <ul class="m-0 list-none space-y-xs p-0">
                        @for (i of c.informes; track i.id) {
                          <li class="flex items-center justify-between gap-md rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-sm"
                              [attr.data-testid]="'resultado-informe-' + i.id">
                            <div>
                              <span class="font-semibold text-[var(--color-on-surface)]">Informe No. {{ i.numero }}</span>
                              <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· {{ i.estado }}</span>
                              <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· {{ i.fechaInicio }} a {{ i.fechaFin }}</span>
                              @if (i.revisorNombre) {
                                <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· Rev: {{ i.revisorNombre }}</span>
                              }
                            </div>
                            <button
                              class="flex-shrink-0 text-xs font-semibold text-[var(--color-primary)] hover:underline"
                              type="button"
                              (click)="irAInforme(i.id)"
                            >Ver informe</button>
                          </li>
                        }
                      </ul>
                    </div>
                  }
                </article>
              }
            </div>
          }
        </section>
      }

      <!-- Resultados legacy (búsqueda simple T8) — se mantiene para compatibilidad -->
      @if (resultados()) {
        <!-- Contratistas -->
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg" data-testid="seccion-contratistas">
          <div class="mb-md flex items-center gap-sm">
            <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
            <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">
              Contratistas
              <span class="ml-sm text-xs font-normal text-[var(--color-on-surface-variant)]">
                ({{ resultados()!.contratistas.length }})
              </span>
            </h3>
          </div>
          @if (resultados()!.contratistas.length === 0) {
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin resultados.</p>
          } @else {
            <ul class="m-0 list-none space-y-xs p-0">
              @for (c of resultados()!.contratistas; track c.id) {
                <li class="flex items-center justify-between gap-md rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-sm"
                    [attr.data-testid]="'resultado-contratista-' + c.id">
                  <div>
                    <span class="font-semibold text-[var(--color-on-surface)]">{{ c.nombre }}</span>
                    <span class="ml-sm text-[var(--color-on-surface-variant)]">{{ c.email }}</span>
                    @if (c.cargo) {
                      <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· {{ c.cargo }}</span>
                    }
                  </div>
                  <button
                    class="text-xs font-semibold text-[var(--color-primary)] hover:underline"
                    type="button"
                    (click)="irAUsuarios()"
                  >Ver usuarios</button>
                </li>
              }
            </ul>
          }
        </section>

        <!-- Contratos -->
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg" data-testid="seccion-contratos">
          <div class="mb-md flex items-center gap-sm">
            <span class="h-5 w-1 rounded-full bg-[var(--color-secondary-container)]"></span>
            <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">
              Contratos
              <span class="ml-sm text-xs font-normal text-[var(--color-on-surface-variant)]">
                ({{ resultados()!.contratos.length }})
              </span>
            </h3>
          </div>
          @if (resultados()!.contratos.length === 0) {
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin resultados.</p>
          } @else {
            <ul class="m-0 list-none space-y-xs p-0">
              @for (c of resultados()!.contratos; track c.id) {
                <li class="flex items-center justify-between gap-md rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-sm"
                    [attr.data-testid]="'resultado-contrato-' + c.id">
                  <div>
                    <span class="font-semibold text-[var(--color-on-surface)]">{{ c.numero }}</span>
                    <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">{{ c.estado }}</span>
                    @if (c.contratistaNombre) {
                      <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· {{ c.contratistaNombre }}</span>
                    }
                    <p class="m-0 mt-xs text-xs text-[var(--color-on-surface-variant)]">{{ c.objeto }}</p>
                  </div>
                  <button
                    class="text-xs font-semibold text-[var(--color-primary)] hover:underline"
                    type="button"
                    (click)="irAContrato(c.id)"
                  >Ver detalle</button>
                </li>
              }
            </ul>
          }
        </section>

        <!-- Informes -->
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg" data-testid="seccion-informes">
          <div class="mb-md flex items-center gap-sm">
            <span class="h-5 w-1 rounded-full bg-[var(--color-error)]"></span>
            <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">
              Informes
              <span class="ml-sm text-xs font-normal text-[var(--color-on-surface-variant)]">
                ({{ resultados()!.informes.length }})
              </span>
            </h3>
          </div>
          @if (resultados()!.informes.length === 0) {
            <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin resultados.</p>
          } @else {
            <ul class="m-0 list-none space-y-xs p-0">
              @for (i of resultados()!.informes; track i.id) {
                <li class="flex items-center justify-between gap-md rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-sm"
                    [attr.data-testid]="'resultado-informe-' + i.id">
                  <div>
                    <span class="font-semibold text-[var(--color-on-surface)]">
                      Informe No. {{ i.numero }}
                    </span>
                    @if (i.contratoNumero) {
                      <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· {{ i.contratoNumero }}</span>
                    }
                    <span class="ml-sm text-xs text-[var(--color-on-surface-variant)]">· {{ i.estado }}</span>
                    <p class="m-0 mt-xs text-xs text-[var(--color-on-surface-variant)]">
                      {{ i.fechaInicio }} a {{ i.fechaFin }}
                      @if (i.contratistaNombre) { · {{ i.contratistaNombre }} }
                    </p>
                  </div>
                  <button
                    class="text-xs font-semibold text-[var(--color-primary)] hover:underline"
                    type="button"
                    (click)="irAInforme(i.id)"
                  >Ver detalle</button>
                </li>
              }
            </ul>
          }
        </section>
      }
    </div>
  `
})
export class AdminBusquedaComponent {
  readonly estadosContrato = ESTADOS_CONTRATO;
  readonly estadosInforme = ESTADOS_INFORME;

  termino = '';
  fechaInicio = '';
  fechaFin = '';
  estadoContrato = '';
  estadoInforme = '';

  readonly buscando = signal(false);
  readonly error = signal('');
  /** Resultados de la búsqueda avanzada T11. */
  readonly resultadosAvanzados = signal<BusquedaAdminPageResponse | null>(null);
  /** Resultados legacy T8 (solo texto libre). */
  readonly resultados = signal<BusquedaAdminResponse | null>(null);
  readonly paginaActual = signal(0);

  constructor(
    private readonly busquedaService: BusquedaAdminService,
    private readonly router: Router
  ) {}

  buscar(): void {
    this.error.set('');
    this.buscando.set(true);
    this.paginaActual.set(0);
    this.resultados.set(null);
    this.resultadosAvanzados.set(null);
    this._ejecutarBusquedaAvanzada(0);
  }

  irAPagina(pagina: number): void {
    this.paginaActual.set(pagina);
    this.buscando.set(true);
    this._ejecutarBusquedaAvanzada(pagina);
  }

  private _ejecutarBusquedaAvanzada(pagina: number): void {
    const filtros: BusquedaAdminFiltros = {
      q: this.termino,
      pagina,
      tamano: 20
    };
    if (this.estadoContrato) filtros.estadoContrato = this.estadoContrato;
    if (this.estadoInforme) filtros.estadoInforme = this.estadoInforme;
    if (this.fechaInicio) filtros.fechaInicio = this.fechaInicio;
    if (this.fechaFin) filtros.fechaFin = this.fechaFin;

    this.busquedaService.buscarAvanzado(filtros).subscribe({
      next: (resp) => {
        this.resultadosAvanzados.set(resp);
        this.buscando.set(false);
      },
      error: () => {
        this.buscando.set(false);
        this.error.set('No se pudo ejecutar la búsqueda. Intente de nuevo.');
      }
    });
  }

  irAContrato(id: number): void {
    void this.router.navigate(['/contratos', id]);
  }

  irAInforme(id: number): void {
    void this.router.navigate(['/informes', id]);
  }

  irAUsuarios(): void {
    void this.router.navigate(['/admin/usuarios']);
  }
}
