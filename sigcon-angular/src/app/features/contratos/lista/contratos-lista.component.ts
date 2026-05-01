import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

import { EstadoContrato, ContratoResumen } from '../../../core/models/contrato.model';
import { Page } from '../../../core/models/page.model';
import { ContratoService } from '../../../core/services/contrato.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-contratos-lista',
  standalone: true,
  imports: [FormsModule, StatusChipComponent],
  template: `
    <div class="space-y-lg">

      <!-- Page header -->
      <div class="flex items-end justify-between">
        <div>
          <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Mis Contratos</h2>
          <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">Consulta y administra tus contratos activos</p>
        </div>
      </div>

      <!-- Filter bar -->
      <div class="flex flex-wrap gap-sm">
        <div class="flex h-10 items-center gap-sm rounded border border-[var(--color-outline-variant)] bg-white px-sm">
          <svg class="h-4 w-4 text-[var(--color-outline)]" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input
            class="w-56 border-none bg-transparent text-sm outline-none placeholder:text-[var(--color-outline)]"
            placeholder="Buscar por número u objeto..."
            type="text"
            [(ngModel)]="busqueda"
            (ngModelChange)="onBusquedaChange($event)"
          />
        </div>

        <select
          class="h-10 rounded border border-[var(--color-outline-variant)] bg-white px-sm text-sm text-[var(--color-on-surface)] outline-none"
          [(ngModel)]="filtroEstado"
          (ngModelChange)="onFiltroChange()"
        >
          <option value="">Todos los estados</option>
          <option value="EN_EJECUCION">En ejecución</option>
          <option value="LIQUIDADO">Liquidado</option>
          <option value="CERRADO">Cerrado</option>
        </select>
      </div>

      <!-- Table -->
      <div class="overflow-hidden rounded-xl border border-[var(--color-outline-variant)] bg-white">
        <table class="w-full border-collapse text-[13px]">
          <thead class="border-b border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)]">
            <tr>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Número</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Objeto</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Inicio</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fin</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Valor COP</th>
              <th class="px-md py-sm text-right text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Acción</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--color-outline-variant)]">
            @if (cargando()) {
              <tr>
                <td colspan="7" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">Cargando...</td>
              </tr>
            } @else if ((pagina()?.content ?? []).length === 0) {
              <tr>
                <td colspan="7" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">No se encontraron contratos.</td>
              </tr>
            } @else {
              @for (c of pagina()!.content; track c.id) {
                <tr
                  class="cursor-pointer transition-colors hover:bg-[var(--color-surface-container-low)]"
                  (click)="irDetalle(c.id)"
                >
                  <td class="px-md py-sm font-semibold text-[var(--color-primary)]">{{ c.numero }}</td>
                  <td class="max-w-xs truncate px-md py-sm text-[var(--color-on-surface)]">{{ c.objeto }}</td>
                  <td class="px-md py-sm">
                    <app-status-chip [value]="c.estado" [label]="estadoLabel(c.estado)" [tone]="estadoTone(c.estado)" />
                  </td>
                  <td class="px-md py-sm text-[var(--color-on-surface-variant)]">{{ c.fechaInicio }}</td>
                  <td class="px-md py-sm text-[var(--color-on-surface-variant)]">{{ c.fechaFin }}</td>
                  <td class="px-md py-sm text-[var(--color-on-surface-variant)]">{{ formatValor(c.valorTotal) }}</td>
                  <td class="px-md py-sm text-right">
                    <button
                      class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-primary)] hover:bg-[var(--color-surface-container-low)]"
                      type="button"
                      (click)="$event.stopPropagation(); irDetalle(c.id)"
                    >
                      Ver
                    </button>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      @if (pagina(); as p) {
        @if (p.totalPages > 1) {
          <div class="flex items-center justify-between text-sm">
            <span class="text-[var(--color-on-surface-variant)]">
              Página {{ p.number + 1 }} de {{ p.totalPages }} — {{ p.totalElements }} contratos
            </span>
            <div class="flex gap-xs">
              <button
                class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs font-semibold text-[var(--color-primary)] disabled:opacity-40"
                type="button"
                [disabled]="p.first"
                (click)="cambiarPagina(paginaActual() - 1)"
              >Anterior</button>
              <button
                class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs font-semibold text-[var(--color-primary)] disabled:opacity-40"
                type="button"
                [disabled]="p.last"
                (click)="cambiarPagina(paginaActual() + 1)"
              >Siguiente</button>
            </div>
          </div>
        }
      }
    </div>
  `
})
export class ContratosListaComponent implements OnInit {
  readonly pagina = signal<Page<ContratoResumen> | null>(null);
  readonly cargando = signal(true);
  readonly paginaActual = signal(0);

  busqueda = '';
  filtroEstado: EstadoContrato | '' = '';

  private readonly busquedaSubject = new Subject<string>();

  constructor(
    private readonly contratoService: ContratoService,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.busquedaSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
      this.paginaActual.set(0);
      this.cargar();
    });
    this.cargar();
  }

  onBusquedaChange(value: string) {
    this.busquedaSubject.next(value);
  }

  onFiltroChange() {
    this.paginaActual.set(0);
    this.cargar();
  }

  cambiarPagina(num: number) {
    this.paginaActual.set(num);
    this.cargar();
  }

  irDetalle(id: number) {
    void this.router.navigate(['/contratos', id]);
  }

  estadoLabel(estado: EstadoContrato): string {
    const map: Record<EstadoContrato, string> = {
      EN_EJECUCION: 'En ejecución',
      LIQUIDADO: 'Liquidado',
      CERRADO: 'Cerrado'
    };
    return map[estado];
  }

  estadoTone(estado: EstadoContrato): 'neutral' | 'success' | 'warning' | 'danger' {
    if (estado === 'EN_EJECUCION') return 'success';
    if (estado === 'LIQUIDADO') return 'warning';
    return 'neutral';
  }

  formatValor(valor: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(valor);
  }

  private cargar() {
    this.cargando.set(true);
    this.contratoService.listarContratos({
      page: this.paginaActual(),
      size: 10,
      estado: this.filtroEstado || undefined,
      search: this.busqueda || undefined
    }).subscribe({
      next: (p) => { this.pagina.set(p); this.cargando.set(false); },
      error: () => { this.cargando.set(false); }
    });
  }
}
