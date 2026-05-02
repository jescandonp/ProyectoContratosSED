import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { Notificacion } from '../../../core/models/notificacion.model';
import { Page } from '../../../core/models/page.model';
import { NotificacionService } from '../../../core/services/notificacion.service';

@Component({
  selector: 'app-centro-notificaciones',
  standalone: true,
  template: `
    <div class="space-y-lg">
      <header class="flex flex-col gap-sm md:flex-row md:items-end md:justify-between">
        <div>
          <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">Notificaciones</p>
          <h1 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">Centro de Notificaciones</h1>
          <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">Eventos recientes de informes y revisiones.</p>
        </div>
        <button
          class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)] disabled:opacity-50"
          type="button"
          [disabled]="procesandoTodas()"
          data-testid="marcar-todas"
          (click)="marcarTodas()"
        >
          Marcar todas como leidas
        </button>
      </header>

      @if (error()) {
        <div class="rounded border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      <div class="overflow-hidden rounded border border-[var(--color-outline-variant)] bg-white">
        @if (cargando()) {
          <div class="p-xl text-center text-sm text-[var(--color-on-surface-variant)]">Cargando notificaciones...</div>
        } @else if ((page()?.content?.length ?? 0) === 0) {
          <div class="p-xl text-center text-sm text-[var(--color-on-surface-variant)]">No hay notificaciones.</div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full border-collapse text-left">
              <thead>
                <tr class="border-b border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)]">
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Tipo</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Titulo</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Descripcion</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fecha</th>
                  <th class="px-md py-sm text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--color-outline-variant)]">
                @for (notificacion of page()!.content; track notificacion.id) {
                  <tr
                    class="cursor-pointer transition-colors hover:bg-[var(--color-surface-container-low)]"
                    [class.bg-[var(--color-primary-container)]]="!notificacion.leida"
                    data-testid="notificacion-row"
                    (click)="abrir(notificacion)"
                  >
                    <td class="px-md py-sm text-[var(--color-primary)]">
                      <i [class]="icono(notificacion.tipoEvento)" aria-hidden="true"></i>
                    </td>
                    <td class="px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]">{{ notificacion.titulo }}</td>
                    <td class="px-md py-sm text-sm text-[var(--color-on-surface-variant)]">{{ notificacion.descripcion }}</td>
                    <td class="px-md py-sm text-sm text-[var(--color-on-surface-variant)]">{{ fechaRelativa(notificacion.fecha) }}</td>
                    <td class="px-md py-sm">
                      <span
                        class="rounded-full px-sm py-1 text-xs font-bold"
                        [class.bg-[var(--color-primary-container)]]="!notificacion.leida"
                        [class.text-[var(--color-on-primary-container)]]="!notificacion.leida"
                        [class.bg-[var(--color-surface-container-low)]]="notificacion.leida"
                        [class.text-[var(--color-on-surface-variant)]]="notificacion.leida"
                      >
                        {{ notificacion.leida ? 'Leida' : 'No leida' }}
                      </span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <div class="flex items-center justify-between border-t border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-sm">
            <span class="text-[var(--color-on-surface-variant)]">
              Pagina {{ paginaActual() + 1 }} de {{ page()!.totalPages || 1 }}
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
export class CentroNotificacionesComponent implements OnInit {
  readonly page = signal<Page<Notificacion> | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly paginaActual = signal(0);
  readonly procesandoTodas = signal(false);

  private readonly tamanioPagina = 10;

  constructor(
    private readonly notificacionService: NotificacionService,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.notificacionService.listar(this.paginaActual(), this.tamanioPagina).subscribe({
      next: (page) => {
        this.page.set(page);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las notificaciones.');
        this.cargando.set(false);
      }
    });
  }

  cambiarPagina(pagina: number) {
    this.paginaActual.set(pagina);
    this.cargar();
  }

  abrir(notificacion: Notificacion) {
    const navegar = () => {
      if (notificacion.idInforme !== null) {
        void this.router.navigate(['/informes', notificacion.idInforme]);
      }
    };

    if (notificacion.leida) {
      navegar();
      return;
    }

    this.notificacionService.marcarLeida(notificacion.id).subscribe({
      next: navegar,
      error: navegar
    });
  }

  marcarTodas() {
    this.procesandoTodas.set(true);
    this.notificacionService.marcarTodasLeidas().subscribe({
      next: () => {
        this.procesandoTodas.set(false);
        this.cargar();
      },
      error: () => {
        this.procesandoTodas.set(false);
        this.error.set('No se pudieron marcar las notificaciones como leidas.');
      }
    });
  }

  fechaRelativa(fecha: string) {
    const timestamp = new Date(fecha).getTime();
    if (Number.isNaN(timestamp)) {
      return fecha;
    }
    const minutos = Math.max(0, Math.floor((Date.now() - timestamp) / 60000));
    if (minutos < 1) return 'Ahora';
    if (minutos < 60) return `Hace ${minutos} min`;
    const horas = Math.floor(minutos / 60);
    if (horas < 24) return `Hace ${horas} h`;
    return `Hace ${Math.floor(horas / 24)} d`;
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
