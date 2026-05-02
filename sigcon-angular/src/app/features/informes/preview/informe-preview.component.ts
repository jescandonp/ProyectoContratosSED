import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { EstadoInforme, InformeDetalle } from '../../../core/models/informe.model';
import { InformeService } from '../../../core/services/informe.service';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-informe-preview',
  standalone: true,
  imports: [StatusChipComponent],
  template: `
    <div class="space-y-lg">
      <nav class="flex items-center gap-xs text-sm text-[var(--color-on-surface-variant)]">
        <button class="text-[var(--color-primary)] hover:underline" type="button" (click)="volver()">Informe</button>
        <span>/</span>
        <span>Vista Previa</span>
      </nav>

      @if (error()) {
        <div class="rounded-xl border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-lg py-md text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      @if (informe(); as i) {
        <div class="flex items-center justify-between">
          <div>
            <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">Vista Previa</p>
            <h2 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">Informe No. {{ i.numero }}</h2>
          </div>
          <button class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]" type="button" (click)="volver()">
            Volver
          </button>
        </div>

        <article class="mx-auto max-w-5xl rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl shadow-sm">
          <header class="border-b border-[var(--color-outline-variant)] pb-lg">
            <div class="flex flex-col gap-md md:flex-row md:items-start md:justify-between">
              <div>
                <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">SIGCON</p>
                <h1 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">Informe mensual de actividades</h1>
                <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">{{ i.contratoNumero }}</p>
              </div>
              <app-status-chip [value]="i.estado" [label]="estadoLabel(i.estado)" [tone]="estadoTone(i.estado)" />
            </div>
          </header>

          <section class="grid grid-cols-1 gap-md border-b border-[var(--color-outline-variant)] py-lg md:grid-cols-3">
            <div>
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Periodo</p>
              <p class="m-0 mt-xs text-sm text-[var(--color-on-surface)]">{{ i.fechaInicio }} a {{ i.fechaFin }}</p>
            </div>
            <div>
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contratista</p>
              <p class="m-0 mt-xs text-sm text-[var(--color-on-surface)]">{{ i.contratista?.nombre ?? 'No asignado' }}</p>
            </div>
            <div>
              <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Supervisor</p>
              <p class="m-0 mt-xs text-sm text-[var(--color-on-surface)]">{{ i.supervisor?.nombre ?? 'No asignado' }}</p>
            </div>
          </section>

          <section class="py-lg">
            <h3 class="m-0 mb-md text-base font-semibold text-[var(--color-on-surface)]">Actividades reportadas</h3>
            <div class="space-y-md">
              @for (actividad of i.actividades; track actividad.id) {
                <div class="rounded-lg border border-[var(--color-outline-variant)] p-md">
                  <div class="flex items-start gap-sm">
                    <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-bold text-white">
                      {{ actividad.ordenObligacion ?? '-' }}
                    </span>
                    <div class="min-w-0 flex-1">
                      <p class="m-0 text-sm font-semibold text-[var(--color-on-surface)]">{{ actividad.descripcionObligacion ?? 'Obligación contractual' }}</p>
                      <p class="m-0 mt-xs text-sm leading-6 text-[var(--color-on-surface-variant)]">{{ actividad.descripcion }}</p>
                    </div>
                    <span class="rounded-full bg-[var(--color-surface-container-low)] px-sm py-xs text-xs font-bold text-[var(--color-primary)]">{{ actividad.porcentaje }}%</span>
                  </div>
                  @if (actividad.soportes.length > 0) {
                    <div class="mt-sm flex flex-wrap gap-xs pl-10">
                      @for (soporte of actividad.soportes; track soporte.id) {
                        <span class="rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-sm py-xs text-xs text-[var(--color-on-surface)]">
                          {{ soporte.nombre }}: {{ soporte.referencia }}
                        </span>
                      }
                    </div>
                  }
                </div>
              }
            </div>
          </section>

          <section class="border-t border-[var(--color-outline-variant)] pt-lg">
            <h3 class="m-0 mb-md text-base font-semibold text-[var(--color-on-surface)]">Documentos adicionales</h3>
            @if (i.documentosAdicionales.length === 0) {
              <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">Sin documentos adicionales registrados.</p>
            } @else {
              <table class="w-full border-collapse text-left text-sm">
                <thead>
                  <tr class="border-b border-[var(--color-outline-variant)] text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
                    <th class="py-sm">Documento</th>
                    <th class="py-sm">Referencia</th>
                  </tr>
                </thead>
                <tbody>
                  @for (doc of i.documentosAdicionales; track doc.id) {
                    <tr class="border-b border-[var(--color-outline-variant)]">
                      <td class="py-sm text-[var(--color-on-surface)]">{{ doc.nombreCatalogo ?? 'Documento' }}</td>
                      <td class="py-sm text-[var(--color-on-surface-variant)]">{{ doc.referencia }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </section>
        </article>
      } @else if (!error()) {
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Cargando vista previa...
        </div>
      }
    </div>
  `
})
export class InformePreviewComponent implements OnInit {
  readonly informe = signal<InformeDetalle | null>(null);
  readonly error = signal('');

  constructor(
    private readonly informeService: InformeService,
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
      next: (informe) => this.informe.set(informe),
      error: () => this.error.set('No se pudo cargar la vista previa del informe.')
    });
  }

  volver() {
    const informe = this.informe();
    void this.router.navigate(informe ? ['/informes', informe.id] : ['/contratos']);
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
