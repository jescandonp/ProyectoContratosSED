import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import {
  BusquedaAdminResponse,
  BusquedaAdminService,
  ContratistaResultado,
  ContratoResultado,
  InformeResultado
} from '../../../core/services/busqueda-admin.service';

@Component({
  selector: 'app-admin-busqueda',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="space-y-lg">
      <div>
        <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Búsqueda global</h2>
        <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">
          Busca contratistas, contratos e informes. Filtra por rango de periodo del informe.
        </p>
      </div>

      <!-- Formulario de búsqueda -->
      <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
        <div class="grid grid-cols-1 gap-md lg:grid-cols-[1fr_auto_auto_auto]">
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Texto libre
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
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Periodo desde
            </span>
            <input
              class="h-9 rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none focus:border-[var(--color-primary)]"
              type="date"
              data-testid="input-fecha-inicio"
              [(ngModel)]="fechaInicio"
            />
          </label>
          <label class="block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Periodo hasta
            </span>
            <input
              class="h-9 rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none focus:border-[var(--color-primary)]"
              type="date"
              data-testid="input-fecha-fin"
              [(ngModel)]="fechaFin"
            />
          </label>
          <div class="flex items-end">
            <button
              class="h-9 rounded bg-[var(--color-primary)] px-lg text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
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
  termino = '';
  fechaInicio = '';
  fechaFin = '';

  readonly buscando = signal(false);
  readonly error = signal('');
  readonly resultados = signal<BusquedaAdminResponse | null>(null);

  constructor(
    private readonly busquedaService: BusquedaAdminService,
    private readonly router: Router
  ) {}

  buscar(): void {
    this.error.set('');
    this.buscando.set(true);
    this.busquedaService.buscar(
      this.termino,
      this.fechaInicio || undefined,
      this.fechaFin || undefined
    ).subscribe({
      next: (resp) => {
        this.resultados.set(resp);
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
