import { SlicePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { InformeResumen } from '../../core/models/informe.model';
import { Page } from '../../core/models/page.model';
import { InformeService } from '../../core/services/informe.service';

@Component({
  selector: 'app-cola-visto-bueno',
  standalone: true,
  imports: [SlicePipe],
  template: `
    <div class="space-y-lg">
      <header class="flex flex-col gap-sm md:flex-row md:items-end md:justify-between">
        <div>
          <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">Visto Bueno Administrativo</p>
          <h1 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">Cola de Visto Bueno</h1>
          <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">Informes en espera de visto bueno administrativo.</p>
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

      <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white overflow-hidden">
        @if (cargando()) {
          <div class="p-xl text-center text-sm text-[var(--color-on-surface-variant)]">Cargando informes...</div>
        } @else if ((page()?.content?.length ?? 0) === 0) {
          <div class="p-xl text-center text-sm text-[var(--color-on-surface-variant)]">No hay informes en espera de visto bueno.</div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="border-b border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)]">
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contrato</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Informe</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Periodo</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Enviado</th>
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
                    <td class="px-md py-sm text-right">
                      <button
                        class="rounded bg-[var(--color-primary)] px-sm py-xs text-xs font-semibold text-white"
                        type="button"
                        (click)="verDetalle(informe)"
                      >
                        Revisar
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

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
  `
})
export class ColaVistoBuenoComponent implements OnInit {
  readonly page = signal<Page<InformeResumen> | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly paginaActual = signal(0);

  readonly tamanioPagina = 10;

  constructor(
    private readonly informeService: InformeService,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.informeService.listarColaVistoBueno(this.paginaActual(), this.tamanioPagina).subscribe({
      next: (page) => {
        this.page.set(page);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la cola de visto bueno.');
        this.cargando.set(false);
      }
    });
  }

  cambiarPagina(pagina: number) {
    this.paginaActual.set(pagina);
    this.cargar();
  }

  verDetalle(informe: InformeResumen) {
    void this.router.navigate(['/visto-bueno', informe.id]);
  }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }
}
