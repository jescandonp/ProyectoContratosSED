import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ContratoResumen, EstadoContrato } from '../../../core/models/contrato.model';
import { Page } from '../../../core/models/page.model';
import { ContratoService } from '../../../core/services/contrato.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-admin-contratos',
  standalone: true,
  imports: [FormsModule, StatusChipComponent],
  template: `
    <div class="space-y-lg">

      <!-- Page header -->
      <div class="flex items-end justify-between">
        <div>
          <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Gestión de Contratos</h2>
          <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">Administra todos los contratos del sistema</p>
        </div>
        <button
          class="flex items-center gap-xs rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white hover:opacity-90"
          type="button"
          (click)="nuevo()"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Nuevo contrato
        </button>
      </div>

      <!-- Filters -->
      <div class="flex flex-wrap gap-sm">
        <select
          class="h-10 rounded border border-[var(--color-outline-variant)] bg-white px-sm text-sm text-[var(--color-on-surface)] outline-none"
          [(ngModel)]="filtroEstado"
          (ngModelChange)="cargar()"
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
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contratista</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fin</th>
              <th class="px-md py-sm text-right text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--color-outline-variant)]">
            @if (cargando()) {
              <tr><td colspan="6" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">Cargando...</td></tr>
            } @else if ((pagina()?.content ?? []).length === 0) {
              <tr><td colspan="6" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">No se encontraron contratos.</td></tr>
            } @else {
              @for (c of pagina()!.content; track c.id) {
                <tr class="transition-colors hover:bg-[var(--color-surface-container-low)]">
                  <td class="px-md py-sm font-semibold text-[var(--color-primary)]">{{ c.numero }}</td>
                  <td class="max-w-xs truncate px-md py-sm text-[var(--color-on-surface)]">{{ c.objeto }}</td>
                  <td class="px-md py-sm text-[var(--color-on-surface-variant)]">{{ c.contratistaNombre }}</td>
                  <td class="px-md py-sm">
                    <app-status-chip [value]="c.estado" [label]="estadoLabel(c.estado)" [tone]="estadoTone(c.estado)" />
                  </td>
                  <td class="px-md py-sm text-[var(--color-on-surface-variant)]">{{ c.fechaFin }}</td>
                  <td class="px-md py-sm text-right">
                    <div class="flex justify-end gap-xs">
                      <button
                        class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-primary)] hover:bg-[var(--color-surface-container-low)]"
                        type="button"
                        (click)="editar(c.id)"
                      >Editar</button>
                      <button
                        class="rounded border border-[var(--color-error-container)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-error)] hover:bg-[var(--color-error-container)]"
                        type="button"
                        (click)="eliminar(c)"
                      >Eliminar</button>
                    </div>
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
            <span class="text-[var(--color-on-surface-variant)]">Página {{ p.number + 1 }} de {{ p.totalPages }}</span>
            <div class="flex gap-xs">
              <button
                class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs font-semibold text-[var(--color-primary)] disabled:opacity-40"
                [disabled]="p.first" (click)="cambiarPagina(paginaActual() - 1)"
              >Anterior</button>
              <button
                class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs font-semibold text-[var(--color-primary)] disabled:opacity-40"
                [disabled]="p.last" (click)="cambiarPagina(paginaActual() + 1)"
              >Siguiente</button>
            </div>
          </div>
        }
      }
    </div>
  `
})
export class AdminContratosComponent implements OnInit {
  readonly pagina = signal<Page<ContratoResumen> | null>(null);
  readonly cargando = signal(true);
  readonly paginaActual = signal(0);

  filtroEstado: EstadoContrato | '' = '';

  constructor(
    private readonly contratoService: ContratoService,
    private readonly router: Router
  ) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.contratoService.listarContratos({
      page: this.paginaActual(), size: 15,
      estado: this.filtroEstado || undefined
    }).subscribe({
      next: (p) => { this.pagina.set(p); this.cargando.set(false); },
      error: () => this.cargando.set(false)
    });
  }

  cambiarPagina(num: number) { this.paginaActual.set(num); this.cargar(); }
  nuevo() { void this.router.navigate(['/admin/contratos/nuevo']); }
  editar(id: number) { void this.router.navigate(['/admin/contratos', id, 'editar']); }

  eliminar(c: ContratoResumen) {
    if (!confirm(`¿Eliminar el contrato ${c.numero}? Esta acción es irreversible.`)) return;
    this.contratoService.eliminarContrato(c.id).subscribe(() => this.cargar());
  }

  estadoLabel(estado: EstadoContrato): string {
    const map: Record<EstadoContrato, string> = { EN_EJECUCION: 'En ejecución', LIQUIDADO: 'Liquidado', CERRADO: 'Cerrado' };
    return map[estado];
  }
  estadoTone(estado: EstadoContrato): 'neutral' | 'success' | 'warning' | 'danger' {
    if (estado === 'EN_EJECUCION') return 'success';
    if (estado === 'LIQUIDADO') return 'warning';
    return 'neutral';
  }
}
