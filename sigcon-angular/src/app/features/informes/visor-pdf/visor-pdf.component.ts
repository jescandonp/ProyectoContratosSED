import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { InformeDetalle } from '../../../core/models/informe.model';
import { InformeService } from '../../../core/services/informe.service';
import { PdfInformeService } from '../../../core/services/pdf-informe.service';

@Component({
  selector: 'app-visor-pdf',
  standalone: true,
  template: `
    <div class="mx-auto max-w-3xl space-y-lg">
      <nav class="flex items-center gap-xs text-sm text-[var(--color-on-surface-variant)]">
        <button class="text-[var(--color-primary)] hover:underline" type="button" (click)="volverAlInforme()">Informe</button>
        <span>/</span>
        <span>PDF aprobado</span>
      </nav>

      @if (informe(); as i) {
        <section class="rounded border border-[var(--color-outline-variant)] bg-white p-lg">
          <p class="m-0 text-xs font-bold uppercase tracking-wider text-[var(--color-primary)]">{{ i.contratoNumero }}</p>
          <h1 class="m-0 mt-xs text-2xl font-bold text-[var(--color-on-surface)]">Informe No. {{ i.numero }}</h1>
          <dl class="mt-md grid grid-cols-1 gap-sm text-sm md:grid-cols-3">
            <div>
              <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</dt>
              <dd class="m-0 mt-xs text-[var(--color-on-surface)]">{{ i.estado }}</dd>
            </div>
            <div>
              <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Aprobacion</dt>
              <dd class="m-0 mt-xs text-[var(--color-on-surface)]">{{ i.fechaAprobacion ?? 'No registrada' }}</dd>
            </div>
            <div>
              <dt class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Hash PDF</dt>
              <dd class="m-0 mt-xs truncate text-[var(--color-on-surface)]">{{ i.pdfHash ?? 'No registrado' }}</dd>
            </div>
          </dl>
        </section>
      }

      @if (mensajePdf()) {
        <div class="rounded border border-[var(--color-secondary-container)] bg-[var(--color-secondary-fixed)] px-lg py-md text-sm font-semibold text-[var(--color-secondary)]">
          {{ mensajePdf() }}
        </div>
      }

      @if (pdfDisponible()) {
        <section class="rounded border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="flex flex-col gap-md md:flex-row md:items-center md:justify-between">
            <div>
              <h2 class="m-0 text-lg font-bold text-[var(--color-on-surface)]">PDF generado</h2>
              <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">Documento inmutable generado al aprobar el informe.</p>
            </div>
            <button
              class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white"
              type="button"
              data-testid="download-pdf"
              (click)="descargarPdf()"
            >
              Descargar PDF
            </button>
          </div>
        </section>
      } @else if (!mensajePdf()) {
        <div class="rounded border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Cargando PDF...
        </div>
      }
    </div>
  `
})
export class VisorPdfComponent implements OnInit {
  readonly informe = signal<InformeDetalle | null>(null);
  readonly pdfDisponible = signal(false);
  readonly mensajePdf = signal('');

  private idInforme = 0;
  private pdfBlob: Blob | null = null;

  constructor(
    private readonly informeService: InformeService,
    private readonly pdfInformeService: PdfInformeService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    this.idInforme = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.idInforme) {
      this.mensajePdf.set('No se pudo identificar el informe solicitado.');
      return;
    }

    this.informeService.obtenerDetalle(this.idInforme).subscribe({
      next: (informe) => this.informe.set(informe),
      error: () => this.mensajePdf.set('No se pudo cargar la informacion del informe.')
    });

    this.pdfInformeService.descargar(this.idInforme).subscribe({
      next: (blob) => {
        this.pdfBlob = blob;
        this.pdfDisponible.set(true);
        this.mensajePdf.set('');
      },
      error: () => {
        this.pdfBlob = null;
        this.pdfDisponible.set(false);
        this.mensajePdf.set('El PDF no esta disponible. El informe debe estar en estado APROBADO.');
      }
    });
  }

  descargarPdf() {
    if (!this.pdfBlob) return;

    const informe = this.informe();
    const url = URL.createObjectURL(this.pdfBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `informe-${informe?.numero ?? this.idInforme}.pdf`;
    link.click();
    URL.revokeObjectURL(url);
  }

  volverAlInforme() {
    if (this.idInforme) {
      void this.router.navigate(['/informes', this.idInforme]);
    }
  }
}
