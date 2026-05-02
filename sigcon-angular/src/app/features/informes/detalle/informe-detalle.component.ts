import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { EstadoInforme, InformeDetalle } from '../../../core/models/informe.model';
import { InformeService } from '../../../core/services/informe.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-informe-detalle',
  standalone: true,
  imports: [StatusChipComponent],
  template: `
    <div class="space-y-lg">
      <nav class="flex items-center gap-xs text-sm text-[var(--color-on-surface-variant)]">
        <button class="text-[var(--color-primary)] hover:underline" type="button" (click)="volverAContratos()">Contratos</button>
        <span>/</span>
        <span>{{ informe()?.contratoNumero ?? 'Informe' }}</span>
      </nav>

      @if (error()) {
        <div class="rounded-xl border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      @if (informe(); as i) {
        <section class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="flex flex-col gap-md lg:flex-row lg:items-start lg:justify-between">
            <div>
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">{{ i.contratoNumero }}</p>
              <h2 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">Informe No. {{ i.numero }}</h2>
              <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">{{ i.fechaInicio }} a {{ i.fechaFin }}</p>
            </div>
            <div class="flex flex-wrap items-center gap-sm">
              <app-status-chip [value]="i.estado" [label]="estadoLabel(i.estado)" [tone]="estadoTone(i.estado)" />
              <button class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]" type="button" (click)="verPreview()">
                Vista previa
              </button>
              @if (i.estado === 'APROBADO' && i.pdfRuta) {
                <button
                  class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white"
                  type="button"
                  data-testid="ver-pdf"
                  (click)="verPdf()"
                >
                  Ver / Descargar PDF
                </button>
              }
              @if (puedeEnviar(i.estado)) {
                <button class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white" type="button" (click)="enviar()">
                  Enviar
                </button>
              }
            </div>
          </div>
        </section>

        <div class="grid grid-cols-1 gap-lg lg:grid-cols-3">
          <section class="space-y-lg lg:col-span-2">
            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <div class="mb-md flex items-center gap-sm">
                <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
                <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Actividades reportadas</h3>
              </div>
              <div class="space-y-md">
                @for (actividad of i.actividades; track actividad.id) {
                  <article class="rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-md">
                    <div class="mb-sm flex items-start gap-sm">
                      <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-bold text-white">
                        {{ actividad.ordenObligacion ?? '-' }}
                      </span>
                      <div>
                        <p class="m-0 text-sm font-semibold text-[var(--color-on-surface)]">{{ actividad.descripcionObligacion ?? 'Obligación contractual' }}</p>
                        <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">{{ actividad.descripcion }}</p>
                      </div>
                      <span class="ml-auto rounded-full bg-white px-sm py-xs text-xs font-bold text-[var(--color-primary)]">{{ actividad.porcentaje }}%</span>
                    </div>
                    @if (actividad.soportes.length > 0) {
                      <div class="mt-sm flex flex-wrap gap-xs">
                        @for (soporte of actividad.soportes; track soporte.id) {
                          <span class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs text-[var(--color-on-surface)]">
                            {{ soporte.nombre }} · {{ soporte.tipo }}
                          </span>
                        }
                      </div>
                    }
                  </article>
                }
              </div>
            </div>

            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <div class="mb-md flex items-center gap-sm">
                <span class="h-5 w-1 rounded-full bg-[var(--color-secondary-container)]"></span>
                <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Documentos adicionales</h3>
              </div>
              @if (i.documentosAdicionales.length === 0) {
                <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin documentos adicionales registrados.</p>
              } @else {
                <ul class="m-0 list-none space-y-sm p-0">
                  @for (doc of i.documentosAdicionales; track doc.id) {
                    <li class="flex items-center justify-between gap-md rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-sm">
                      <span class="font-semibold text-[var(--color-on-surface)]">{{ doc.nombreCatalogo ?? 'Documento' }}</span>
                      <span class="text-[var(--color-on-surface-variant)]">{{ doc.referencia }}</span>
                    </li>
                  }
                </ul>
              }
            </div>
          </section>

          <aside class="space-y-lg">
            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Actores</h3>
              <dl class="space-y-md text-sm">
                <div>
                  <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contratista</dt>
                  <dd class="m-0 mt-xs text-[var(--color-on-surface)]">{{ i.contratista?.nombre ?? 'No asignado' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Supervisor</dt>
                  <dd class="m-0 mt-xs text-[var(--color-on-surface)]">{{ i.supervisor?.nombre ?? 'No asignado' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Revisor</dt>
                  <dd class="m-0 mt-xs text-[var(--color-on-surface)]">{{ i.revisor?.nombre ?? 'No asignado' }}</dd>
                </div>
              </dl>
            </div>

            <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
              <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Observaciones</h3>
              @if (i.observaciones.length === 0) {
                <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin observaciones registradas.</p>
              } @else {
                <ul class="m-0 list-none space-y-sm p-0">
                  @for (observacion of i.observaciones; track observacion.id) {
                    <li class="rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-sm text-sm">
                      <p class="m-0 font-semibold text-[var(--color-on-surface)]">{{ observacion.autorRol }}</p>
                      <p class="m-0 mt-xs text-[var(--color-on-surface-variant)]">{{ observacion.texto }}</p>
                    </li>
                  }
                </ul>
              }
            </div>
          </aside>
        </div>
      } @else if (!error()) {
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Cargando informe...
        </div>
      }
    </div>
  `
})
export class InformeDetalleComponent implements OnInit {
  readonly informe = signal<InformeDetalle | null>(null);
  readonly error = signal('');

  constructor(
    private readonly informeService: InformeService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('ID de informe inválido.');
      return;
    }
    this.informeService.obtenerDetalle(id).subscribe({
      next: (informe) => this.informe.set(informe),
      error: () => this.error.set('No se pudo cargar el informe.')
    });
  }

  verPreview() {
    const informe = this.informe();
    if (informe) void this.router.navigate(['/informes', informe.id, 'preview']);
  }

  verPdf() {
    const informe = this.informe();
    if (informe) void this.router.navigate(['/informes', informe.id, 'pdf']);
  }

  enviar() {
    const informe = this.informe();
    if (!informe || !this.puedeEnviar(informe.estado)) return;
    if (!window.confirm('¿Desea enviar este informe para revisión?')) return;

    this.informeService.enviarInforme(informe.id).subscribe({
      next: (actualizado) => this.informe.set(actualizado),
      error: () => this.error.set('No se pudo enviar el informe.')
    });
  }

  volverAContratos() {
    void this.router.navigate(['/contratos']);
  }

  puedeEnviar(estado: EstadoInforme) {
    return estado === 'BORRADOR' || estado === 'DEVUELTO';
  }

  estadoLabel(estado: EstadoInforme): string {
    const map: Record<EstadoInforme, string> = {
      BORRADOR: 'Borrador',
      ENVIADO: 'Enviado',
      EN_REVISION: 'En revisión',
      DEVUELTO: 'Devuelto',
      APROBADO: 'Aprobado'
    };
    return map[estado];
  }

  estadoTone(estado: EstadoInforme): 'neutral' | 'success' | 'warning' | 'danger' {
    if (estado === 'APROBADO') return 'success';
    if (estado === 'DEVUELTO') return 'danger';
    if (estado === 'BORRADOR') return 'neutral';
    return 'warning';
  }
}
