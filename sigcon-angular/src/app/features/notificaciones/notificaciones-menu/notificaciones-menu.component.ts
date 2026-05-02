import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { Notificacion } from '../../../core/models/notificacion.model';
import { NotificacionService } from '../../../core/services/notificacion.service';

@Component({
  selector: 'app-notificaciones-menu',
  standalone: true,
  template: `
    <div class="relative">
      <button
        class="relative flex h-9 w-9 items-center justify-center rounded border border-[var(--color-outline-variant)] bg-white text-[var(--color-on-surface)] hover:bg-[var(--color-surface-container-low)]"
        type="button"
        aria-label="Notificaciones"
        data-testid="notificaciones-toggle"
        (click)="alternarMenu()"
      >
        <i class="pi pi-bell text-base" aria-hidden="true"></i>
        @if (countNoLeidas() > 0) {
          <span
            class="absolute -right-1 -top-1 min-w-5 rounded-full bg-[var(--color-error)] px-1.5 py-0.5 text-center text-[10px] font-bold leading-none text-white"
            data-testid="notificaciones-badge"
          >
            {{ countNoLeidas() }}
          </span>
        }
      </button>

      @if (abierto()) {
        <div
          class="absolute right-0 z-40 mt-xs w-80 overflow-hidden rounded border border-[var(--color-outline-variant)] bg-white shadow-lg"
          data-testid="notificaciones-overlay"
        >
          <div class="flex items-center justify-between border-b border-[var(--color-outline-variant)] px-md py-sm">
            <span class="text-sm font-bold text-[var(--color-on-surface)]">Notificaciones</span>
            <button
              class="text-xs font-semibold text-[var(--color-primary)] hover:underline"
              type="button"
              (click)="verTodas()"
            >
              Ver todas
            </button>
          </div>

          @if (cargando()) {
            <div class="p-md text-sm text-[var(--color-on-surface-variant)]">Cargando...</div>
          } @else if (recientes().length === 0) {
            <div class="p-md text-sm text-[var(--color-on-surface-variant)]">No hay notificaciones.</div>
          } @else {
            <div class="max-h-96 overflow-y-auto">
              @for (notificacion of recientes(); track notificacion.id) {
                <button
                  class="block w-full border-b border-[var(--color-outline-variant)] px-md py-sm text-left hover:bg-[var(--color-surface-container-low)]"
                  [class.bg-[var(--color-primary-container)]]="!notificacion.leida"
                  type="button"
                  data-testid="notificacion-menu-item"
                  (click)="abrirNotificacion(notificacion)"
                >
                  <div class="flex items-start gap-sm">
                    <span class="mt-0.5 text-[var(--color-primary)]">
                      <i [class]="icono(notificacion.tipoEvento)" aria-hidden="true"></i>
                    </span>
                    <span class="min-w-0 flex-1">
                      <span class="block truncate text-sm font-semibold text-[var(--color-on-surface)]">{{ notificacion.titulo }}</span>
                      <span class="mt-0.5 line-clamp-2 block text-xs text-[var(--color-on-surface-variant)]">{{ notificacion.descripcion }}</span>
                    </span>
                  </div>
                </button>
              }
            </div>
          }
        </div>
      }
    </div>
  `
})
export class NotificacionesMenuComponent implements OnInit, OnDestroy {
  readonly countNoLeidas = signal(0);
  readonly recientes = signal<Notificacion[]>([]);
  readonly abierto = signal(false);
  readonly cargando = signal(false);

  private polling?: Subscription;

  constructor(
    private readonly notificacionService: NotificacionService,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.polling = this.notificacionService
      .pollNoLeidas()
      .subscribe((response) => this.countNoLeidas.set(response.count));
  }

  ngOnDestroy() {
    this.polling?.unsubscribe();
  }

  alternarMenu() {
    this.abierto.set(!this.abierto());
    if (this.abierto()) {
      this.cargarRecientes();
    }
  }

  cargarRecientes() {
    this.cargando.set(true);
    this.notificacionService.listar(0, 5).subscribe({
      next: (page) => {
        this.recientes.set(page.content);
        this.cargando.set(false);
      },
      error: () => {
        this.recientes.set([]);
        this.cargando.set(false);
      }
    });
  }

  abrirNotificacion(notificacion: Notificacion) {
    const navegar = () => {
      this.abierto.set(false);
      if (notificacion.idInforme !== null) {
        void this.router.navigate(['/informes', notificacion.idInforme]);
      }
    };

    if (notificacion.leida) {
      navegar();
      return;
    }

    this.notificacionService.marcarLeida(notificacion.id).subscribe({
      next: () => {
        this.recientes.update((items) =>
          items.map((item) => item.id === notificacion.id ? { ...item, leida: true } : item)
        );
        this.countNoLeidas.update((count) => Math.max(0, count - 1));
        navegar();
      },
      error: navegar
    });
  }

  verTodas() {
    this.abierto.set(false);
    void this.router.navigate(['/notificaciones']);
  }

  icono(tipoEvento: Notificacion['tipoEvento']) {
    switch (tipoEvento) {
      case 'INFORME_APROBADO':
      case 'REVISION_APROBADA':
        return 'pi pi-check-circle';
      case 'INFORME_DEVUELTO':
      case 'REVISION_DEVUELTA':
        return 'pi pi-exclamation-circle';
      default:
        return 'pi pi-send';
    }
  }
}
