import { SlicePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { InformeResumen } from '../../core/models/informe.model';
import { Page } from '../../core/models/page.model';
import { InformeService } from '../../core/services/informe.service';
import { ObservacionService } from '../../core/services/observacion.service';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-cola-revision',
  standalone: true,
  imports: [FormsModule, StatusChipComponent, SlicePipe],
  template: `
    <div class="space-y-lg">
      <header class="flex flex-col gap-sm md:flex-row md:items-end md:justify-between">
        <div>
          <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">Revisión de informes</p>
          <h1 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">Cola de Revisión</h1>
          <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">Informes enviados asignados a su revisión.</p>
        </div>
        <div class="flex items-center gap-sm text-sm text-[var(--color-on-surface-variant)]">
          <span class="font-bold text-[var(--color-primary)]">{{ page()?.totalElements ?? 0 }}</span>
          <span>informes pendientes</span>
        </div>
      </header>

      @if (error()) {
        <div class="rounded-xl border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      <!-- Filtros -->
      <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-md">
        <div class="flex flex-wrap items-end gap-md">
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contrato</span>
            <input
              class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm w-48"
              type="text"
              placeholder="Número de contrato"
              [ngModel]="filtroContrato()"
              (ngModelChange)="filtroContrato.set($event)"
            />
          </label>
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contratista</span>
            <input
              class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm w-48"
              type="text"
              placeholder="Nombre del contratista"
              [ngModel]="filtroContratista()"
              (ngModelChange)="filtroContratista.set($event)"
            />
          </label>
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Periodo desde</span>
            <input
              class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
              type="date"
              [ngModel]="filtroPeriodoDesde()"
              (ngModelChange)="filtroPeriodoDesde.set($event)"
            />
          </label>
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Periodo hasta</span>
            <input
              class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
              type="date"
              [ngModel]="filtroPeriodoHasta()"
              (ngModelChange)="filtroPeriodoHasta.set($event)"
            />
          </label>
          <button
            class="rounded bg-[var(--color-primary)] px-md py-xs text-sm font-semibold text-white"
            type="button"
            (click)="aplicarFiltros()"
          >
            Filtrar
          </button>
          <button
            class="rounded border border-[var(--color-outline-variant)] px-md py-xs text-sm font-semibold text-[var(--color-on-surface)]"
            type="button"
            (click)="limpiarFiltros()"
          >
            Limpiar
          </button>
        </div>
      </section>

      <!-- Tabla -->
      <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white overflow-hidden">
        @if (cargando()) {
          <div class="p-xl text-center text-sm text-[var(--color-on-surface-variant)]">Cargando informes...</div>
        } @else if ((page()?.content?.length ?? 0) === 0) {
          <div class="p-xl text-center text-sm text-[var(--color-on-surface-variant)]">No hay informes enviados asignados a su revisión.</div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="border-b border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)]">
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contrato</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Informe</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Periodo</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Enviado</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)] text-right">Acciones</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--color-outline-variant)]">
                @for (informe of page()!.content; track informe.id) {
                  <tr class="hover:bg-[var(--color-surface-container-low)] transition-colors">
                    <td class="px-md py-sm text-sm font-semibold text-[var(--color-primary)]">{{ informe.contratoNumero ?? '—' }}</td>
                    <td class="px-md py-sm text-sm text-[var(--color-on-surface)]">No. {{ informe.numero }}</td>
                    <td class="px-md py-sm text-sm text-[var(--color-on-surface-variant)]">{{ informe.fechaInicio }} — {{ informe.fechaFin }}</td>
                    <td class="px-md py-sm text-sm text-[var(--color-on-surface-variant)]">{{ informe.fechaUltimoEnvio ? (informe.fechaUltimoEnvio | slice:0:10) : '—' }}</td>
                    <td class="px-md py-sm">
                      <app-status-chip value="ENVIADO" label="Enviado" tone="warning" />
                    </td>
                    <td class="px-md py-sm text-right">
                      <div class="flex items-center justify-end gap-xs">
                        <button
                          class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-xs font-semibold text-[var(--color-on-surface)] hover:bg-[var(--color-surface-container-low)]"
                          type="button"
                          (click)="verDetalle(informe)"
                        >
                          Ver detalle
                        </button>
                        <button
                          class="rounded bg-[var(--color-primary)] px-sm py-xs text-xs font-semibold text-white disabled:opacity-50"
                          type="button"
                          [disabled]="procesando() === informe.id"
                          (click)="aprobarRevision(informe)"
                        >
                          Aprobar revisión
                        </button>
                        <button
                          class="rounded border border-[var(--color-error)] px-sm py-xs text-xs font-semibold text-[var(--color-error)] disabled:opacity-50"
                          type="button"
                          [disabled]="procesando() === informe.id"
                          (click)="abrirDialogoDevolucion(informe)"
                        >
                          Devolver
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Paginación -->
          <div class="flex items-center justify-between border-t border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-sm">
            <span class="text-[var(--color-on-surface-variant)]">
              {{ (paginaActual() * tamanioPagina) + 1 }}–{{ min((paginaActual() + 1) * tamanioPagina, page()!.totalElements) }}
              de {{ page()!.totalElements }}
            </span>
            <div class="flex gap-xs">
              <button
                class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-xs font-semibold disabled:opacity-40"
                type="button"
                [disabled]="page()!.first"
                (click)="cambiarPagina(paginaActual() - 1)"
              >
                Anterior
              </button>
              <button
                class="rounded border border-[var(--color-outline-variant)] px-sm py-xs text-xs font-semibold disabled:opacity-40"
                type="button"
                [disabled]="page()!.last"
                (click)="cambiarPagina(paginaActual() + 1)"
              >
                Siguiente
              </button>
            </div>
          </div>
        }
      </div>
    </div>

    <!-- Diálogo de devolución -->
    @if (dialogoDevolucion()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--color-on-surface)]/40">
        <div class="w-full max-w-lg rounded-xl bg-white p-lg shadow-xl">
          <h2 class="m-0 text-lg font-bold text-[var(--color-on-surface)]">Devolver informe</h2>
          <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">
            Informe No. {{ dialogoDevolucion()!.numero }} — {{ dialogoDevolucion()!.contratoNumero }}
          </p>

          <label class="mt-md block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Observación <span class="text-[var(--color-error)]">*</span>
            </span>
            <textarea
              class="min-h-28 w-full resize-y rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
              placeholder="Indique los motivos de la devolución..."
              [ngModel]="textoObservacion()"
              (ngModelChange)="textoObservacion.set($event)"
            ></textarea>
            @if (errorObservacion()) {
              <p class="mt-xs text-xs text-[var(--color-error)]">{{ errorObservacion() }}</p>
            }
          </label>

          <div class="mt-md flex justify-end gap-sm">
            <button
              class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]"
              type="button"
              (click)="cerrarDialogo()"
            >
              Cancelar
            </button>
            <button
              class="rounded border border-[var(--color-error)] bg-[var(--color-error)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50"
              type="button"
              [disabled]="procesando() !== null"
              (click)="confirmarDevolucion()"
            >
              Confirmar devolución
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ColaRevisionComponent implements OnInit {
  readonly page = signal<Page<InformeResumen> | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly procesando = signal<number | null>(null);
  readonly paginaActual = signal(0);

  readonly filtroContrato = signal('');
  readonly filtroContratista = signal('');
  readonly filtroPeriodoDesde = signal('');
  readonly filtroPeriodoHasta = signal('');

  readonly dialogoDevolucion = signal<InformeResumen | null>(null);
  readonly textoObservacion = signal('');
  readonly errorObservacion = signal('');

  readonly tamanioPagina = 10;

  constructor(
    private readonly informeService: InformeService,
    private readonly observacionService: ObservacionService,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.informeService.listarInformes({ page: this.paginaActual(), size: this.tamanioPagina }).subscribe({
      next: (page) => {
        this.page.set(page);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la cola de revisión.');
        this.cargando.set(false);
      }
    });
  }

  aplicarFiltros() {
    this.paginaActual.set(0);
    this.cargar();
  }

  limpiarFiltros() {
    this.filtroContrato.set('');
    this.filtroContratista.set('');
    this.filtroPeriodoDesde.set('');
    this.filtroPeriodoHasta.set('');
    this.paginaActual.set(0);
    this.cargar();
  }

  cambiarPagina(pagina: number) {
    this.paginaActual.set(pagina);
    this.cargar();
  }

  verDetalle(informe: InformeResumen) {
    void this.router.navigate(['/informes', informe.id]);
  }

  aprobarRevision(informe: InformeResumen) {
    this.procesando.set(informe.id);
    this.error.set('');
    this.observacionService.aprobarRevision(informe.id).subscribe({
      next: () => {
        this.procesando.set(null);
        this.cargar();
      },
      error: () => {
        this.procesando.set(null);
        this.error.set('No se pudo aprobar la revisión del informe.');
      }
    });
  }

  abrirDialogoDevolucion(informe: InformeResumen) {
    this.dialogoDevolucion.set(informe);
    this.textoObservacion.set('');
    this.errorObservacion.set('');
  }

  cerrarDialogo() {
    this.dialogoDevolucion.set(null);
    this.textoObservacion.set('');
    this.errorObservacion.set('');
  }

  confirmarDevolucion() {
    const informe = this.dialogoDevolucion();
    if (!informe) return;

    const texto = this.textoObservacion().trim();
    if (!texto) {
      this.errorObservacion.set('La observación es obligatoria para devolver el informe.');
      return;
    }

    this.procesando.set(informe.id);
    this.errorObservacion.set('');
    this.observacionService.devolverRevision(informe.id, { texto }).subscribe({
      next: () => {
        this.procesando.set(null);
        this.cerrarDialogo();
        this.cargar();
      },
      error: () => {
        this.procesando.set(null);
        this.error.set('No se pudo devolver el informe.');
      }
    });
  }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }
}
