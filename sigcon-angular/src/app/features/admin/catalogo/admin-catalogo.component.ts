import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DocumentoCatalogo, DocumentoCatalogoRequest } from '../../../core/models/documento-catalogo.model';
import { Page } from '../../../core/models/page.model';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';

@Component({
  selector: 'app-admin-catalogo',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="space-y-lg">

      <!-- Header -->
      <div class="flex items-end justify-between">
        <div>
          <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Catálogo de Documentos</h2>
          <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">Documentos requeridos para contratos de tipo OPS</p>
        </div>
        <button
          class="flex items-center gap-xs rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white hover:opacity-90"
          type="button"
          (click)="abrirFormulario(null)"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Nuevo documento
        </button>
      </div>

      <!-- Table -->
      <div class="overflow-hidden rounded-xl border border-[var(--color-outline-variant)] bg-white">
        <table class="w-full border-collapse text-[13px]">
          <thead class="border-b border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)]">
            <tr>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Nombre</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Descripción</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Tipo</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Obligatorio</th>
              <th class="px-md py-sm text-right text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--color-outline-variant)]">
            @if (cargando()) {
              <tr><td colspan="5" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">Cargando...</td></tr>
            } @else if ((pagina()?.content ?? []).length === 0) {
              <tr><td colspan="5" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">No hay documentos en el catálogo.</td></tr>
            } @else {
              @for (d of pagina()!.content; track d.id) {
                <tr class="transition-colors hover:bg-[var(--color-surface-container-low)]">
                  <td class="px-md py-sm font-semibold text-[var(--color-on-surface)]">{{ d.nombre }}</td>
                  <td class="max-w-xs truncate px-md py-sm text-[var(--color-on-surface-variant)]">{{ d.descripcion ?? '—' }}</td>
                  <td class="px-md py-sm">
                    <span class="rounded border border-[var(--color-outline-variant)] px-xs py-[2px] text-[11px] font-bold uppercase text-[var(--color-primary)]">{{ d.tipoContrato }}</span>
                  </td>
                  <td class="px-md py-sm">
                    <span [class]="d.obligatorio ? 'text-[var(--color-error)] font-semibold' : 'text-[var(--color-on-surface-variant)]'">
                      {{ d.obligatorio ? 'Sí' : 'No' }}
                    </span>
                  </td>
                  <td class="px-md py-sm text-right">
                    <div class="flex justify-end gap-xs">
                      <button
                        class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-primary)] hover:bg-[var(--color-surface-container-low)]"
                        type="button"
                        (click)="abrirFormulario(d)"
                      >Editar</button>
                      <button
                        class="rounded border border-[var(--color-error-container)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-error)] hover:bg-[var(--color-error-container)]"
                        type="button"
                        (click)="eliminar(d)"
                      >Eliminar</button>
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      @if (pagina(); as p) {
        @if (p.totalPages > 1) {
          <div class="flex items-center justify-between text-sm">
            <span class="text-[var(--color-on-surface-variant)]">Página {{ p.number + 1 }} de {{ p.totalPages }}</span>
            <div class="flex gap-xs">
              <button class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs font-semibold text-[var(--color-primary)] disabled:opacity-40"
                [disabled]="p.first" (click)="cambiarPagina(paginaActual() - 1)">Anterior</button>
              <button class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs font-semibold text-[var(--color-primary)] disabled:opacity-40"
                [disabled]="p.last" (click)="cambiarPagina(paginaActual() + 1)">Siguiente</button>
            </div>
          </div>
        }
      }

      <!-- Inline form modal -->
      @if (mostrarFormulario()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm" (click)="cerrarFormulario()">
          <div class="w-full max-w-md rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg shadow-xl" (click)="$event.stopPropagation()">
            <h3 class="mb-md text-lg font-semibold text-[var(--color-on-surface)]">
              {{ editando() ? 'Editar documento' : 'Nuevo documento' }}
            </h3>
            <form class="space-y-md" (ngSubmit)="guardar()" #formD="ngForm">
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Nombre *</label>
                <input class="input-field" type="text" required [(ngModel)]="form.nombre" name="nombre" />
              </div>
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Descripción</label>
                <input class="input-field" type="text" [(ngModel)]="form.descripcion" name="descripcion" />
              </div>
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Tipo contrato</label>
                <input class="input-field" type="text" value="OPS" disabled />
              </div>
              <div class="flex items-center gap-sm">
                <input type="checkbox" id="obligatorio" [(ngModel)]="form.obligatorio" name="obligatorio" class="h-4 w-4 rounded border-[var(--color-outline-variant)] text-[var(--color-primary)]" />
                <label for="obligatorio" class="text-sm text-[var(--color-on-surface)]">Documento obligatorio</label>
              </div>
              @if (errorForm()) {
                <p class="text-xs text-[var(--color-error)]">{{ errorForm() }}</p>
              }
              <div class="flex justify-end gap-sm pt-sm">
                <button type="button" class="rounded border border-[var(--color-outline-variant)] bg-white px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]" (click)="cerrarFormulario()">Cancelar</button>
                <button type="submit" class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50" [disabled]="guardando() || formD.invalid">
                  {{ guardando() ? 'Guardando...' : 'Guardar' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>

    <style>
      .input-field {
        height: 2.5rem; width: 100%; border-radius: 0.25rem;
        border: 1px solid var(--color-outline-variant);
        background: var(--color-surface-bright); padding: 0 0.5rem; font-size: 0.875rem; outline: none;
      }
      .input-field:focus { border-color: var(--color-primary); box-shadow: 0 0 0 1px var(--color-primary); }
      .input-field:disabled { opacity: 0.6; cursor: not-allowed; background: var(--color-surface-container-low); }
    </style>
  `
})
export class AdminCatalogoComponent implements OnInit {
  readonly pagina = signal<Page<DocumentoCatalogo> | null>(null);
  readonly cargando = signal(true);
  readonly paginaActual = signal(0);
  readonly mostrarFormulario = signal(false);
  readonly editando = signal<DocumentoCatalogo | null>(null);
  readonly guardando = signal(false);
  readonly errorForm = signal('');

  form: DocumentoCatalogoRequest = { nombre: '', descripcion: null, obligatorio: false, tipoContrato: 'OPS' };

  constructor(private readonly catalogoService: DocumentoCatalogoService) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.catalogoService.listar({ page: this.paginaActual(), size: 15 })
      .subscribe({ next: (p) => { this.pagina.set(p); this.cargando.set(false); }, error: () => this.cargando.set(false) });
  }

  cambiarPagina(num: number) { this.paginaActual.set(num); this.cargar(); }

  abrirFormulario(d: DocumentoCatalogo | null) {
    this.editando.set(d);
    this.errorForm.set('');
    this.form = d
      ? { nombre: d.nombre, descripcion: d.descripcion, obligatorio: d.obligatorio, tipoContrato: d.tipoContrato }
      : { nombre: '', descripcion: null, obligatorio: false, tipoContrato: 'OPS' };
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardar() {
    this.guardando.set(true);
    this.errorForm.set('');
    const d = this.editando();
    const op = d ? this.catalogoService.actualizar(d.id, this.form) : this.catalogoService.crear(this.form);
    op.subscribe({
      next: () => { this.guardando.set(false); this.cerrarFormulario(); this.cargar(); },
      error: () => { this.guardando.set(false); this.errorForm.set('Error al guardar. Intente de nuevo.'); }
    });
  }

  eliminar(d: DocumentoCatalogo) {
    if (!confirm(`¿Eliminar "${d.nombre}"?`)) return;
    this.catalogoService.eliminar(d.id).subscribe(() => this.cargar());
  }
}
