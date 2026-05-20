import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { InformeDetalle } from '../../core/models/informe.model';
import { InformeService } from '../../core/services/informe.service';
import { ObservacionService } from '../../core/services/observacion.service';

@Component({
  selector: 'app-detalle-visto-bueno',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="space-y-lg">
      <header class="flex flex-col gap-sm md:flex-row md:items-end md:justify-between">
        <div>
          <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">Visto Bueno Administrativo</p>
          <h1 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">
            @if (informe()) {
              Informe No. {{ informe()!.numero }} — {{ informe()!.contratoNumero ?? '' }}
            } @else {
              Cargando...
            }
          </h1>
        </div>
        <button
          class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]"
          type="button"
          (click)="volver()"
        >
          ← Volver a la cola
        </button>
      </header>

      @if (error()) {
        <div class="rounded-xl border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      @if (informe()) {
        <!-- Info del periodo -->
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <h2 class="m-0 mb-md text-base font-bold text-[var(--color-on-surface)]">Datos generales</h2>
          <dl class="grid grid-cols-2 gap-md text-sm md:grid-cols-4">
            <div>
              <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Período</dt>
              <dd class="mt-xs text-[var(--color-on-surface)]">{{ informe()!.fechaInicio }} — {{ informe()!.fechaFin }}</dd>
            </div>
            <div>
              <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</dt>
              <dd class="mt-xs font-semibold" style="color: #7A5100;">En Visto Bueno</dd>
            </div>
            <div>
              <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contratista</dt>
              <dd class="mt-xs text-[var(--color-on-surface)]">{{ informe()!.contratista?.nombre ?? '—' }}</dd>
            </div>
            <div>
              <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Enviado</dt>
              <dd class="mt-xs text-[var(--color-on-surface)]">{{ informe()!.fechaUltimoEnvio?.slice(0, 10) ?? '—' }}</dd>
            </div>
          </dl>
        </section>

        <!-- Actividades (solo lectura) -->
        @if ((informe()!.actividades.length ?? 0) > 0) {
          <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
            <h2 class="m-0 mb-md text-base font-bold text-[var(--color-on-surface)]">Actividades</h2>
            <ul class="space-y-sm">
              @for (act of informe()!.actividades; track act.id) {
                <li class="rounded border border-[var(--color-outline-variant)] p-md text-sm text-[var(--color-on-surface)]">
                  {{ act.descripcion }}
                </li>
              }
            </ul>
          </section>
        }

        <!-- Observaciones previas -->
        @if (informe()!.observaciones.length > 0) {
          <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
            <h2 class="m-0 mb-md text-base font-bold text-[var(--color-on-surface)]">Observaciones anteriores</h2>
            <ul class="space-y-sm">
              @for (obs of informe()!.observaciones; track obs.id) {
                <li class="rounded border border-[var(--color-outline-variant)] p-md text-sm">
                  <p class="m-0 text-[var(--color-on-surface)]">{{ obs.texto }}</p>
                  <p class="m-0 mt-xs text-xs text-[var(--color-on-surface-variant)]">{{ obs.autorRol }} — {{ obs.fecha?.slice(0, 10) }}</p>
                </li>
              }
            </ul>
          </section>
        }

        <!-- Barra de acciones fija -->
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <h2 class="m-0 mb-md text-base font-bold text-[var(--color-on-surface)]">Acciones</h2>
          <div class="flex flex-wrap gap-sm">
            <button
              class="rounded bg-[var(--color-primary)] px-lg py-sm text-sm font-semibold text-white disabled:opacity-50"
              type="button"
              [disabled]="procesando()"
              (click)="darVistosBueno()"
            >
              Dar Visto Bueno
            </button>
            <button
              class="rounded border-2 px-lg py-sm text-sm font-semibold disabled:opacity-50"
              style="border-color: #FFB300; color: #7A5100;"
              type="button"
              [disabled]="procesando()"
              (click)="abrirDialogo('escalar')"
            >
              Escalar a Supervisor
            </button>
            <button
              class="rounded border border-[var(--color-error)] px-lg py-sm text-sm font-semibold text-[var(--color-error)] disabled:opacity-50"
              type="button"
              [disabled]="procesando()"
              (click)="abrirDialogo('devolver')"
            >
              Devolver al Contratista
            </button>
          </div>
        </div>
      }
    </div>

    <!-- Diálogo Escalar / Devolver -->
    @if (dialogoActivo()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--color-on-surface)]/40">
        <div class="w-full max-w-lg rounded-xl bg-white p-lg shadow-xl">
          <h2 class="m-0 text-lg font-bold text-[var(--color-on-surface)]">
            {{ dialogoActivo() === 'escalar' ? 'Escalar a Supervisor' : 'Devolver al Contratista' }}
          </h2>

          <label class="mt-md block">
            <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Observación
              @if (dialogoActivo() === 'devolver') {
                <span class="text-[var(--color-error)]"> *</span>
              }
            </span>
            <textarea
              class="min-h-28 w-full resize-y rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
              [placeholder]="dialogoActivo() === 'devolver' ? 'Indique los motivos de la devolución...' : 'Indique los motivos de la escalación (opcional)...'"
              [ngModel]="textoDialogo()"
              (ngModelChange)="textoDialogo.set($event)"
            ></textarea>
            @if (errorDialogo()) {
              <p class="mt-xs text-xs text-[var(--color-error)]">{{ errorDialogo() }}</p>
            }
          </label>

          <div class="mt-md flex justify-end gap-sm">
            <button
              class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]"
              type="button"
              (click)="cerrarDialogo()"
            >
              Cancelar
            </button>
            <button
              class="rounded px-md py-sm text-sm font-semibold text-white disabled:opacity-50"
              [class.bg-[var(--color-primary)]]="dialogoActivo() === 'escalar'"
              [class.bg-[var(--color-error)]]="dialogoActivo() === 'devolver'"
              type="button"
              [disabled]="procesando()"
              (click)="confirmarDialogo()"
            >
              Confirmar
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class DetalleVistoBuenoComponent implements OnInit {
  readonly informe = signal<InformeDetalle | null>(null);
  readonly error = signal('');
  readonly procesando = signal(false);

  readonly dialogoActivo = signal<'escalar' | 'devolver' | null>(null);
  readonly textoDialogo = signal('');
  readonly errorDialogo = signal('');

  constructor(
    private readonly informeService: InformeService,
    private readonly observacionService: ObservacionService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('ID de informe inválido.');
      return;
    }
    this.informeService.obtenerDetalle(id).subscribe({
      next: (inf) => this.informe.set(inf),
      error: () => this.error.set('No se pudo cargar el informe.')
    });
  }

  darVistosBueno() {
    const inf = this.informe();
    if (!inf) return;
    this.procesando.set(true);
    this.error.set('');
    this.informeService.darVistosBueno(inf.id).subscribe({
      next: () => {
        this.procesando.set(false);
        void this.router.navigate(['/visto-bueno']);
      },
      error: () => {
        this.procesando.set(false);
        this.error.set('No se pudo dar el visto bueno. Intente de nuevo.');
      }
    });
  }

  abrirDialogo(tipo: 'escalar' | 'devolver') {
    this.dialogoActivo.set(tipo);
    this.textoDialogo.set('');
    this.errorDialogo.set('');
  }

  cerrarDialogo() {
    this.dialogoActivo.set(null);
    this.textoDialogo.set('');
    this.errorDialogo.set('');
  }

  confirmarDialogo() {
    const inf = this.informe();
    const tipo = this.dialogoActivo();
    if (!inf || !tipo) return;

    const texto = this.textoDialogo().trim();

    if (tipo === 'devolver' && !texto) {
      this.errorDialogo.set('La observación es obligatoria para devolver el informe.');
      return;
    }

    this.procesando.set(true);
    this.errorDialogo.set('');

    const obs$ = tipo === 'escalar'
      ? this.informeService.escalar(inf.id, texto || undefined)
      : this.observacionService.devolverInforme(inf.id, { texto });

    obs$.subscribe({
      next: () => {
        this.procesando.set(false);
        this.cerrarDialogo();
        void this.router.navigate(['/visto-bueno']);
      },
      error: () => {
        this.procesando.set(false);
        this.errorDialogo.set('No se pudo completar la acción. Intente de nuevo.');
      }
    });
  }

  volver() {
    void this.router.navigate(['/visto-bueno']);
  }
}
