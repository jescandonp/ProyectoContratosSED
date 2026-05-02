import { Component } from '@angular/core';

import { AuthService } from '../../../core/auth/auth.service';
import { NotificacionesMenuComponent } from '../../../features/notificaciones/notificaciones-menu/notificaciones-menu.component';
import { StatusChipComponent } from '../status-chip/status-chip.component';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [StatusChipComponent, NotificacionesMenuComponent],
  template: `
    <header class="flex min-h-16 items-center justify-between border-b border-[var(--color-outline-variant)] bg-white px-lg">
      <div>
        <p class="m-0 text-xs font-bold uppercase tracking-normal text-[var(--color-on-surface-variant)]">Sistema de Gestion de Contratos</p>
        <h2 class="m-0 text-lg font-semibold text-[var(--color-on-surface)]">Panel operativo</h2>
      </div>

      @if (authService.currentUser(); as user) {
        <div class="flex items-center gap-sm">
          <app-notificaciones-menu />
          <app-status-chip [value]="user.rol" [label]="user.rol" tone="success" />
          <div class="text-right">
            <p class="m-0 text-sm font-semibold text-[var(--color-on-surface)]">{{ user.nombre }}</p>
            <p class="m-0 text-xs text-[var(--color-on-surface-variant)]">{{ user.email }}</p>
          </div>
          <button
            class="h-9 rounded border border-[var(--color-outline-variant)] bg-white px-sm text-sm font-semibold text-[var(--color-on-surface)]"
            type="button"
            (click)="authService.logout()"
          >
            Salir
          </button>
        </div>
      }
    </header>
  `
})
export class TopbarComponent {
  constructor(readonly authService: AuthService) {}
}
