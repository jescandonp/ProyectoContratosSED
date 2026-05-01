import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink],
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

        <div class="flex flex-col gap-md rounded-xl border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-lg opacity-60">
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--color-outline)]">
            <svg class="h-5 w-5 text-white" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/>
            </svg>
          </div>
          <div>
            <p class="m-0 text-base font-semibold text-[var(--color-on-surface-variant)]">Informes</p>
            <p class="mt-xs text-xs text-[var(--color-on-surface-variant)]">Disponible en Incremento 2</p>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AdminDashboardComponent {}
