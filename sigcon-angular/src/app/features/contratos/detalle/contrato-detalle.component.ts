import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ContratoDetalle } from '../../../core/models/contrato.model';
import { InformeResumen } from '../../../core/models/informe.model';
import { ContratoService } from '../../../core/services/contrato.service';
import { InformeService } from '../../../core/services/informe.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-contrato-detalle',
  standalone: true,
  imports: [StatusChipComponent, RouterLink],
  template: `
    <div class="space-y-lg">

      <!-- Breadcrumb -->
      <nav class="flex items-center gap-xs text-sm text-[var(--color-on-surface-variant)]">
        <button class="text-[var(--color-primary)] hover:underline" type="button" (click)="volver()">Contratos</button>
        <span>/</span>
        <span>{{ contrato()?.numero ?? '...' }}</span>
      </nav>

      @if (error()) {
        <div class="rounded-xl border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      @if (contrato(); as c) {
        <!-- Header -->
        <div class="flex items-start justify-between">
          <div>
            <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">{{ c.numero }}</h2>
            <p class="mt-xs max-w-2xl text-sm text-[var(--color-on-surface-variant)]">{{ c.objeto }}</p>
          </div>
          <app-status-chip [value]="c.estado" [label]="estadoLabel(c.estado)" [tone]="estadoTone(c.estado)" />
        </div>

        <div class="grid grid-cols-1 gap-lg lg:grid-cols-3">

          <!-- Left: datos generales -->
          <div class="lg:col-span-2 space-y-lg">

            <!-- Datos del contrato -->
            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <div class="mb-md flex items-center gap-sm">
                <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
                <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Datos Generales</h3>
              </div>
              <dl class="grid grid-cols-2 gap-x-lg gap-y-md text-sm">
                <div>
                  <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Tipo</dt>
                  <dd class="mt-xs text-[var(--color-on-surface)]">{{ c.tipo }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Valor Total</dt>
                  <dd class="mt-xs font-semibold text-[var(--color-on-surface)]">{{ formatValor(c.valorTotal) }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fecha Inicio</dt>
                  <dd class="mt-xs text-[var(--color-on-surface)]">{{ c.fechaInicio }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fecha Fin</dt>
                  <dd class="mt-xs text-[var(--color-on-surface)]">{{ c.fechaFin }}</dd>
                </div>
              </dl>
            </div>

            <!-- Obligaciones -->
            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <div class="mb-md flex items-center gap-sm">
                <span class="h-5 w-1 rounded-full bg-[var(--color-secondary-container)]"></span>
                <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Obligaciones Contractuales</h3>
              </div>
              @if (c.obligaciones.length === 0) {
                <p class="text-sm text-[var(--color-on-surface-variant)]">Sin obligaciones registradas.</p>
              } @else {
                <ol class="space-y-sm">
                  @for (o of c.obligaciones; track o.id) {
                    <li class="flex gap-sm rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-sm py-xs text-sm">
                      <span class="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-bold text-white">
                        {{ o.orden }}
                      </span>
                      <span class="text-[var(--color-on-surface)]">{{ o.descripcion }}</span>
                    </li>
                  }
                </ol>
              }
            </div>

            <!-- Historial de informes -->
            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <div class="mb-md flex items-center justify-between">
                <div class="flex items-center gap-sm">
                  <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
                  <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Historial de Informes</h3>
                </div>
                @if (contrato()?.estado === 'EN_EJECUCION') {
                  <a
                    class="rounded bg-[var(--color-primary)] px-md py-xs text-sm font-semibold text-white no-underline"
                    [routerLink]="['/contratos', contrato()!.id, 'informes', 'nuevo']"
                  >
                    + Nuevo Informe
                  </a>
                }
              </div>

              @if (cargandoInformes()) {
                <p class="text-sm text-[var(--color-on-surface-variant)]">Cargando informes...</p>
              } @else if (informes().length === 0) {
                <p class="text-sm text-[var(--color-on-surface-variant)]">
                  Aún no se han registrado informes para este contrato.
                </p>
              } @else {
                <ul class="m-0 list-none space-y-sm p-0">
                  @for (informe of informes(); track informe.id) {
                    <li>
                      <a
                        class="flex items-center justify-between gap-md rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-sm no-underline hover:border-[var(--color-primary)]"
                        [routerLink]="['/informes', informe.id]"
                      >
                        <span class="font-semibold text-[var(--color-on-surface)]">Informe No. {{ informe.numero }}</span>
                        <span class="text-[var(--color-on-surface-variant)]">{{ informe.fechaInicio }} — {{ informe.fechaFin }}</span>
                        <app-status-chip
                          [value]="informe.estado"
                          [label]="estadoInformeLabel(informe.estado)"
                          [tone]="estadoInformeTone(informe.estado)"
                        />
                      </a>
                    </li>
                  }
                </ul>
              }
            </div>
          </div>

          <!-- Right: actores -->
          <div class="space-y-lg">

            <!-- Contratista -->
            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Contratista</h3>
              <div class="space-y-xs text-sm">
                <p class="font-semibold text-[var(--color-on-surface)]">{{ c.contratista.nombre }}</p>
                <p class="text-[var(--color-on-surface-variant)]">{{ c.contratista.email }}</p>
                @if (c.contratista.cargo) {
                  <p class="text-[var(--color-on-surface-variant)]">{{ c.contratista.cargo }}</p>
                }
              </div>
            </div>

            <!-- Supervisor -->
            @if (c.supervisor) {
              <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
                <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Supervisor</h3>
                <div class="space-y-xs text-sm">
                  <p class="font-semibold text-[var(--color-on-surface)]">{{ c.supervisor.nombre }}</p>
                  <p class="text-[var(--color-on-surface-variant)]">{{ c.supervisor.email }}</p>
                </div>
              </div>
            }

            <!-- Revisor -->
            @if (c.revisor) {
              <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
                <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Revisor</h3>
                <div class="space-y-xs text-sm">
                  <p class="font-semibold text-[var(--color-on-surface)]">{{ c.revisor.nombre }}</p>
                  <p class="text-[var(--color-on-surface-variant)]">{{ c.revisor.email }}</p>
                </div>
              </div>
            }

            <!-- Documentos aplicables -->
            @if (c.docsAplicables.length > 0) {
              <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
                <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Documentos Requeridos</h3>
                <ul class="space-y-xs">
                  @for (d of c.docsAplicables; track d.id) {
                    <li class="flex items-center gap-xs text-sm text-[var(--color-on-surface)]">
                      <svg class="h-4 w-4 flex-shrink-0 text-[var(--color-primary)]" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
                      </svg>
                      <span>{{ d.nombre }}</span>
                      @if (d.obligatorio) {
                        <span class="ml-auto text-[10px] font-bold uppercase text-[var(--color-error)]">Obligatorio</span>
                      }
                    </li>
                  }
                </ul>
              </div>
            }
          </div>
        </div>
      } @else if (!error()) {
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Cargando contrato...
        </div>
      }
    </div>
  `
})
export class ContratoDetalleComponent implements OnInit {
  readonly contrato = signal<ContratoDetalle | null>(null);
  readonly informes = signal<InformeResumen[]>([]);
  readonly cargandoInformes = signal(false);
  readonly error = signal('');

  constructor(
    private readonly contratoService: ContratoService,
    private readonly informeService: InformeService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) { this.error.set('ID de contrato inválido.'); return; }
    this.contratoService.obtenerDetalle(id).subscribe({
      next: (c) => {
        this.contrato.set(c);
        this.cargarInformes(id);
      },
      error: () => this.error.set('No se pudo cargar el contrato. Verifique que existe y tiene acceso.')
    });
  }

  cargarInformes(contratoId: number) {
    this.cargandoInformes.set(true);
    this.informeService.listarInformes({ contratoId, size: 50 }).subscribe({
      next: (page) => {
        this.informes.set(page.content);
        this.cargandoInformes.set(false);
      },
      error: () => this.cargandoInformes.set(false)
    });
  }

  volver() { void this.router.navigate(['/contratos']); }

  estadoLabel(estado: string): string {
    const map: Record<string, string> = { EN_EJECUCION: 'En ejecución', LIQUIDADO: 'Liquidado', CERRADO: 'Cerrado' };
    return map[estado] ?? estado;
  }

  estadoTone(estado: string): 'neutral' | 'success' | 'warning' | 'danger' {
    if (estado === 'EN_EJECUCION') return 'success';
    if (estado === 'LIQUIDADO') return 'warning';
    return 'neutral';
  }

  estadoInformeLabel(estado: string): string {
    const map: Record<string, string> = {
      BORRADOR: 'Borrador', ENVIADO: 'Enviado', EN_REVISION: 'En revisión',
      DEVUELTO: 'Devuelto', APROBADO: 'Aprobado'
    };
    return map[estado] ?? estado;
  }

  estadoInformeTone(estado: string): 'neutral' | 'success' | 'warning' | 'danger' {
    if (estado === 'APROBADO') return 'success';
    if (estado === 'DEVUELTO') return 'danger';
    if (estado === 'BORRADOR') return 'neutral';
    return 'warning';
  }

  formatValor(valor: number): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(valor);
  }
}
