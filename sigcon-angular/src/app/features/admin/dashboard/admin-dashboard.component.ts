import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ParametroService } from '../../../core/services/parametro.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink, FormsModule],
  template: `
    <div class="space-y-lg">

      <!-- Page header -->
      <div>
        <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Panel de Administración</h2>
        <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">Gestión centralizada del sistema SIGCON</p>
      </div>

      <!-- Shortcut cards -->
      <div class="grid grid-cols-1 gap-lg sm:grid-cols-2 lg:grid-cols-4">

        <a
          class="flex flex-col gap-md rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg no-underline transition-all hover:border-[var(--color-primary)] hover:shadow-md"
          routerLink="/admin/contratos"
        >
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--color-primary)]">
            <svg class="h-5 w-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
            </svg>
          </div>
          <div>
            <p class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Contratos</p>
            <p class="mt-xs text-xs text-[var(--color-on-surface-variant)]">Crear, editar y administrar contratos OPS</p>
          </div>
        </a>

        <a
          class="flex flex-col gap-md rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg no-underline transition-all hover:border-[var(--color-primary)] hover:shadow-md"
          routerLink="/admin/usuarios"
        >
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--color-primary-container)]">
            <svg class="h-5 w-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <div>
            <p class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Usuarios</p>
            <p class="mt-xs text-xs text-[var(--color-on-surface-variant)]">Gestionar contratistas, revisores y supervisores</p>
          </div>
        </a>

        <a
          class="flex flex-col gap-md rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg no-underline transition-all hover:border-[var(--color-primary)] hover:shadow-md"
          routerLink="/admin/documentos-catalogo"
        >
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--color-secondary-container)]">
            <svg class="h-5 w-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <div>
            <p class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Catálogo Docs</p>
            <p class="mt-xs text-xs text-[var(--color-on-surface-variant)]">Documentos requeridos para contratos OPS</p>
          </div>
        </a>

        <a
          class="flex flex-col gap-md rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg no-underline transition-all hover:border-[var(--color-primary)] hover:shadow-md"
          routerLink="/contratos"
        >
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--color-outline)]">
            <svg class="h-5 w-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/>
            </svg>
          </div>
          <div>
            <p class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Informes</p>
            <p class="mt-xs text-xs text-[var(--color-on-surface-variant)]">Consultar historial de informes por contrato</p>
          </div>
        </a>

        <a
          class="flex flex-col gap-md rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg no-underline transition-all hover:border-[var(--color-primary)] hover:shadow-md"
          routerLink="/admin/busqueda"
        >
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--color-tertiary-container,#e8d5f0)]">
            <svg class="h-5 w-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
          </div>
          <div>
            <p class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Búsqueda global</p>
            <p class="mt-xs text-xs text-[var(--color-on-surface-variant)]">Buscar contratistas, contratos e informes</p>
          </div>
        </a>
      </div>

      <!-- Parámetros del sistema -->
      <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
        <h2 class="m-0 mb-md text-base font-bold text-[var(--color-on-surface)]">Parámetros del sistema</h2>

        @if (errorVb()) {
          <div class="mb-md rounded border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-md py-sm text-sm text-[var(--color-on-error-container)]">
            {{ errorVb() }}
          </div>
        }

        @if (mensajeVb()) {
          <div class="mb-md rounded border border-[var(--color-primary-container)] bg-[var(--color-primary-container)] px-md py-sm text-sm text-[var(--color-on-primary-container)]">
            {{ mensajeVb() }}
          </div>
        }

        <div class="flex items-center gap-md">
          <label class="flex cursor-pointer items-center gap-sm select-none">
            <input
              type="checkbox"
              class="h-5 w-5 cursor-pointer accent-[var(--color-primary)]"
              [ngModel]="vbActivo()"
              (ngModelChange)="onToggleVb($event)"
              [disabled]="cargandoVb()"
            />
            <span class="text-sm font-semibold text-[var(--color-on-surface)]">Visto Bueno Administrativo</span>
          </label>
          <span class="text-xs text-[var(--color-on-surface-variant)]">
            {{ vbActivo() ? 'Activo — los informes revisados pasan por el visto bueno' : 'Inactivo — los informes van directamente al Supervisor' }}
          </span>
        </div>
      </section>

      <!-- Diálogo confirmación desactivar VB -->
      @if (dialogoDesactivar()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--color-on-surface)]/40">
          <div class="w-full max-w-lg rounded-xl bg-white p-lg shadow-xl">
            <h2 class="m-0 text-lg font-bold text-[var(--color-on-surface)]">Desactivar Visto Bueno</h2>
            <p class="mt-sm text-sm text-[var(--color-on-surface-variant)]">
              Los informes en espera de Visto Bueno serán enviados automáticamente al Supervisor. ¿Desea continuar?
            </p>
            <div class="mt-md flex justify-end gap-sm">
              <button
                class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]"
                type="button"
                (click)="cancelarDesactivar()"
              >
                Cancelar
              </button>
              <button
                class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50"
                type="button"
                [disabled]="cargandoVb()"
                (click)="confirmarDesactivar()"
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class AdminDashboardComponent implements OnInit {
  readonly vbActivo = signal(false);
  readonly cargandoVb = signal(false);
  readonly errorVb = signal('');
  readonly mensajeVb = signal('');
  readonly dialogoDesactivar = signal(false);

  constructor(private readonly parametroService: ParametroService) {}

  ngOnInit() {
    this.parametroService.obtenerVb().subscribe({
      next: (p) => this.vbActivo.set(p.activo),
      error: () => this.errorVb.set('No se pudo cargar el estado del parámetro VB.')
    });
  }

  onToggleVb(valor: boolean) {
    if (!valor) {
      this.dialogoDesactivar.set(true);
    } else {
      this.aplicarVb(true);
    }
  }

  cancelarDesactivar() {
    this.dialogoDesactivar.set(false);
  }

  confirmarDesactivar() {
    this.dialogoDesactivar.set(false);
    this.aplicarVb(false);
  }

  private aplicarVb(activo: boolean) {
    this.cargandoVb.set(true);
    this.errorVb.set('');
    this.mensajeVb.set('');
    this.parametroService.setVbActivo(activo).subscribe({
      next: (p) => {
        this.vbActivo.set(p.activo);
        this.cargandoVb.set(false);
        this.mensajeVb.set(activo ? 'Visto Bueno activado.' : 'Visto Bueno desactivado. Informes migrados a revisión.');
      },
      error: () => {
        this.cargandoVb.set(false);
        this.errorVb.set('No se pudo actualizar el parámetro. Intente de nuevo.');
      }
    });
  }
}
