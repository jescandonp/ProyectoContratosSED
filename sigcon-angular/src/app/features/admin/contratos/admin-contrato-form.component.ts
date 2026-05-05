import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { ContratoDetalle, ContratoRequest, TipoContrato } from '../../../core/models/contrato.model';
import { Obligacion } from '../../../core/models/obligacion.model';
import { Usuario } from '../../../core/models/usuario.model';
import { ContratoService } from '../../../core/services/contrato.service';
import { ObligacionService } from '../../../core/services/obligacion.service';
import { UsuarioService } from '../../../core/services/usuario.service';

interface ObligacionForm { id?: number; descripcion: string; orden: number; }

@Component({
  selector: 'app-admin-contrato-form',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="mx-auto max-w-3xl space-y-lg">

      <!-- Breadcrumb -->
      <nav class="flex items-center gap-xs text-sm text-[var(--color-on-surface-variant)]">
        <button class="text-[var(--color-primary)] hover:underline" type="button" (click)="volver()">Contratos</button>
        <span>/</span>
        <span>{{ esEdicion() ? 'Editar contrato' : 'Nuevo contrato' }}</span>
      </nav>

      <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">
        {{ esEdicion() ? 'Editar Contrato' : 'Nuevo Contrato' }}
      </h2>

      @if (error()) {
        <div class="rounded border border-[var(--color-error-container)] bg-[var(--color-error-container)] px-md py-sm text-sm text-[var(--color-on-error-container)]">
          {{ error() }}
        </div>
      }

      <form class="space-y-lg" (ngSubmit)="guardar()" #formRef="ngForm">

        <!-- Datos generales -->
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="mb-md flex items-center gap-sm">
            <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
            <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Información del Contrato</h3>
          </div>
          <div class="grid grid-cols-2 gap-md">
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Número *</label>
              <input class="input-field" type="text" required [(ngModel)]="form.numero" name="numero" />
              @if (errorNumero()) {
                <p class="text-xs text-[var(--color-error)]">{{ errorNumero() }}</p>
              }
            </div>
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Tipo</label>
              <input class="input-field" type="text" value="OPS" disabled />
            </div>
            <div class="col-span-2 space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Objeto Contractual *</label>
              <textarea
                class="w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm py-xs text-sm outline-none focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]"
                rows="3"
                required
                [(ngModel)]="form.objeto"
                name="objeto"
              ></textarea>
            </div>
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Valor Total (COP) *</label>
              <input class="input-field" type="number" min="0" required [(ngModel)]="form.valorTotal" name="valorTotal" />
            </div>
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</label>
              <input class="input-field" type="text" value="EN_EJECUCION" disabled />
            </div>
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fecha Inicio *</label>
              <input class="input-field" type="date" required [(ngModel)]="form.fechaInicio" name="fechaInicio" />
            </div>
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Fecha Fin *</label>
              <input class="input-field" type="date" required [(ngModel)]="form.fechaFin" name="fechaFin" />
            </div>
          </div>
        </div>

        <!-- Asignación de actores -->
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="mb-md flex items-center gap-sm">
            <span class="h-5 w-1 rounded-full bg-[var(--color-primary-container)]"></span>
            <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Asignación de Roles</h3>
          </div>
          <div class="grid grid-cols-3 gap-md">
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Contratista *</label>
              <select class="input-field" required [(ngModel)]="form.idContratista" name="idContratista">
                <option [ngValue]="null">— Seleccionar —</option>
                @for (u of contratistas(); track u.id) {
                  <option [ngValue]="u.id">{{ u.nombre }}</option>
                }
              </select>
            </div>
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Revisor (opcional)</label>
              <select class="input-field" [(ngModel)]="form.idRevisor" name="idRevisor">
                <option [ngValue]="null">— Sin revisor —</option>
                @for (u of revisores(); track u.id) {
                  <option [ngValue]="u.id">{{ u.nombre }}</option>
                }
              </select>
            </div>
            <div class="space-y-xs">
              <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Supervisor *</label>
              <select class="input-field" required [(ngModel)]="form.idSupervisor" name="idSupervisor">
                <option [ngValue]="null">— Seleccionar —</option>
                @for (u of supervisores(); track u.id) {
                  <option [ngValue]="u.id">{{ u.nombre }}</option>
                }
              </select>
            </div>
          </div>
        </div>

        <!-- Obligaciones -->
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="mb-md flex items-center justify-between">
            <div class="flex items-center gap-sm">
              <span class="h-5 w-1 rounded-full bg-[var(--color-secondary-container)]"></span>
              <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Obligaciones Contractuales</h3>
            </div>
            <button
              class="flex items-center gap-xs rounded border border-[var(--color-primary)] px-sm py-xs text-xs font-semibold text-[var(--color-primary)] hover:bg-[var(--color-surface-container-low)]"
              type="button"
              (click)="agregarObligacion()"
            >
              + Añadir
            </button>
          </div>
          <div class="space-y-sm">
            @for (o of obligaciones(); track $index; let i = $index) {
              <div class="flex items-start gap-sm">
                <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-bold text-white">
                  {{ i + 1 }}
                </span>
                <input
                  class="flex-1 rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm py-xs text-sm outline-none focus:border-[var(--color-primary)]"
                  type="text"
                  [placeholder]="'Descripción de la obligación ' + (i + 1)"
                  [(ngModel)]="o.descripcion"
                  [name]="'obligacion_' + i"
                />
                <button
                  class="text-[var(--color-error)] hover:text-[var(--color-on-error-container)]"
                  type="button"
                  (click)="eliminarObligacion(i)"
                >
                  <svg class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                    <path d="M10 11v6M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                  </svg>
                </button>
              </div>
            }
            @if (obligaciones().length === 0) {
              <p class="text-sm text-[var(--color-on-surface-variant)]">Sin obligaciones. Agrega al menos una.</p>
            }
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-sm">
          <button
            class="rounded border border-[var(--color-outline-variant)] bg-white px-lg py-sm text-sm font-semibold text-[var(--color-on-surface)] hover:bg-[var(--color-surface-container-low)]"
            type="button"
            (click)="volver()"
          >
            Cancelar
          </button>
          <button
            class="rounded bg-[var(--color-primary)] px-lg py-sm text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50"
            type="submit"
            [disabled]="guardando() || formRef.invalid"
          >
            {{ guardando() ? 'Guardando...' : (esEdicion() ? 'Guardar cambios' : 'Crear contrato') }}
          </button>
        </div>
      </form>
    </div>

    <style>
      .input-field {
        height: 2.5rem;
        width: 100%;
        border-radius: 0.25rem;
        border: 1px solid var(--color-outline-variant);
        background: var(--color-surface-bright);
        padding: 0 0.5rem;
        font-size: 0.875rem;
        outline: none;
        transition: border-color 0.15s;
      }
      .input-field:focus {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 1px var(--color-primary);
      }
      .input-field:disabled {
        opacity: 0.6;
        cursor: not-allowed;
        background: var(--color-surface-container-low);
      }
    </style>
  `
})
export class AdminContratoFormComponent implements OnInit {
  readonly esEdicion = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly errorNumero = signal('');
  readonly contratistas = signal<Usuario[]>([]);
  readonly revisores = signal<Usuario[]>([]);
  readonly supervisores = signal<Usuario[]>([]);
  readonly obligaciones = signal<ObligacionForm[]>([]);

  form: ContratoRequest = {
    numero: '',
    objeto: '',
    tipo: 'OPS',
    valorTotal: 0,
    fechaInicio: '',
    fechaFin: '',
    idContratista: null as unknown as number,
    idRevisor: null,
    idSupervisor: null as unknown as number
  };

  private contratoId: number | null = null;
  private originalObligacionIds = new Set<number>();

  constructor(
    private readonly contratoService: ContratoService,
    private readonly obligacionService: ObligacionService,
    private readonly usuarioService: UsuarioService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.esEdicion.set(true);
      this.contratoId = Number(id);
      this.contratoService.obtenerDetalle(this.contratoId).subscribe((c) => {
        this.form = {
          numero: c.numero,
          objeto: c.objeto,
          tipo: c.tipo,
          valorTotal: c.valorTotal,
          fechaInicio: c.fechaInicio,
          fechaFin: c.fechaFin,
          idContratista: c.contratista.id,
          idRevisor: c.revisor?.id ?? null,
          idSupervisor: c.supervisor?.id ?? null as unknown as number
        };
        this.obligaciones.set(c.obligaciones.map((o: Obligacion) => ({ id: o.id, descripcion: o.descripcion, orden: o.orden })));
        this.originalObligacionIds = new Set(c.obligaciones.map((o: Obligacion) => o.id));
      });
    }
    this.cargarUsuarios();
  }

  agregarObligacion() {
    const current = this.obligaciones();
    this.obligaciones.set([...current, { descripcion: '', orden: current.length + 1 }]);
  }

  eliminarObligacion(index: number) {
    const current = this.obligaciones();
    const next = current.filter((_, i) => i !== index).map((o, i) => ({ ...o, orden: i + 1 }));
    this.obligaciones.set(next);
  }

  guardar() {
    this.error.set('');
    this.errorNumero.set('');
    this.guardando.set(true);
    const obs = this.esEdicion()
      ? this.contratoService.actualizarContratoAdmin(this.contratoId!, this.form)
      : this.contratoService.crearContrato(this.form);

    obs.pipe(
      switchMap((contrato) => this.sincronizarObligaciones(contrato.id).pipe(map(() => contrato)))
    ).subscribe({
      next: () => void this.router.navigate(['/admin/contratos'], {
        state: { mensaje: this.esEdicion() ? 'Contrato actualizado correctamente.' : 'Contrato creado correctamente.' }
      }),
      error: (err) => {
        this.guardando.set(false);
        const codigo = err?.error?.error ?? '';
        if (codigo === 'NUMERO_CONTRATO_DUPLICADO') {
          this.errorNumero.set('Ya existe un contrato con ese número.');
          this.error.set('Ya existe un contrato con ese número.');
        } else if (codigo === 'VALIDACION_FALLIDA' || codigo === 'USUARIO_NO_ENCONTRADO') {
          this.error.set(err?.error?.mensaje ?? 'Seleccione contratista y supervisor para crear un contrato completo.');
        } else {
          this.error.set('Error al guardar el contrato u obligaciones. Verifique los datos e intente de nuevo.');
        }
      }
    });
  }

  volver() { void this.router.navigate(['/admin/contratos']); }

  private cargarUsuarios() {
    this.usuarioService.listarUsuarios({ rol: 'CONTRATISTA', size: 100 })
      .subscribe((p) => this.contratistas.set(p.content));
    this.usuarioService.listarUsuarios({ rol: 'REVISOR', size: 100 })
      .subscribe((p) => this.revisores.set(p.content));
    this.usuarioService.listarUsuarios({ rol: 'SUPERVISOR', size: 100 })
      .subscribe((p) => this.supervisores.set(p.content));
  }

  private sincronizarObligaciones(contratoId: number) {
    const obligacionesValidas = this.obligaciones()
      .map((obligacion, index) => ({
        ...obligacion,
        descripcion: obligacion.descripcion.trim(),
        orden: index + 1
      }))
      .filter((obligacion) => obligacion.descripcion.length > 0);

    const idsVigentes = new Set(obligacionesValidas.filter((obligacion) => obligacion.id).map((obligacion) => obligacion.id!));
    const eliminaciones = [...this.originalObligacionIds]
      .filter((id) => !idsVigentes.has(id))
      .map((id) => this.obligacionService.eliminar(contratoId, id));
    const guardados = obligacionesValidas.map((obligacion) => {
      const request = { descripcion: obligacion.descripcion, orden: obligacion.orden };
      return obligacion.id
        ? this.obligacionService.actualizar(contratoId, obligacion.id, request)
        : this.obligacionService.crear(contratoId, request);
    });
    const operaciones = [...eliminaciones, ...guardados];
    return operaciones.length === 0 ? of([]) : forkJoin(operaciones);
  }
}
