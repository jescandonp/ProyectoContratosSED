import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { RolUsuario, Usuario, UsuarioRequest } from '../../../core/models/usuario.model';
import { Page } from '../../../core/models/page.model';
import { UsuarioService } from '../../../core/services/usuario.service';

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="space-y-lg">

      <!-- Header -->
      <div class="flex items-end justify-between">
        <div>
          <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Usuarios</h2>
          <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">Gestiona contratistas, revisores, supervisores y administradores</p>
        </div>
        <button
          class="flex items-center gap-xs rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white hover:opacity-90"
          type="button"
          (click)="abrirFormulario(null)"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Nuevo usuario
        </button>
      </div>

      <!-- Filter -->
      <div>
        <select
          class="h-10 rounded border border-[var(--color-outline-variant)] bg-white px-sm text-sm text-[var(--color-on-surface)] outline-none"
          [(ngModel)]="filtroRol"
          (ngModelChange)="cargar()"
        >
          <option value="">Todos los roles</option>
          <option value="ADMIN">Admin</option>
          <option value="CONTRATISTA">Contratista</option>
          <option value="REVISOR">Revisor</option>
          <option value="SUPERVISOR">Supervisor</option>
        </select>
      </div>

      <!-- Table -->
      <div class="overflow-hidden rounded-xl border border-[var(--color-outline-variant)] bg-white">
        <table class="w-full border-collapse text-[13px]">
          <thead class="border-b border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)]">
            <tr>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Nombre</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Email</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Cargo</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Rol</th>
              <th class="px-md py-sm text-left text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Estado</th>
              <th class="px-md py-sm text-right text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--color-outline-variant)]">
            @if (cargando()) {
              <tr><td colspan="6" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">Cargando...</td></tr>
            } @else if ((pagina()?.content ?? []).length === 0) {
              <tr><td colspan="6" class="px-md py-xl text-center text-sm text-[var(--color-on-surface-variant)]">No se encontraron usuarios.</td></tr>
            } @else {
              @for (u of pagina()!.content; track u.id) {
                <tr class="transition-colors hover:bg-[var(--color-surface-container-low)]">
                  <td class="px-md py-sm font-semibold text-[var(--color-on-surface)]">{{ u.nombre }}</td>
                  <td class="px-md py-sm text-[var(--color-on-surface-variant)]">{{ u.email }}</td>
                  <td class="px-md py-sm text-[var(--color-on-surface-variant)]">{{ u.cargo ?? '—' }}</td>
                  <td class="px-md py-sm">
                    <span class="rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-sm py-xs text-xs font-bold uppercase text-[var(--color-primary)]">
                      {{ u.rol }}
                    </span>
                  </td>
                  <td class="px-md py-sm">
                    <span [class]="u.activo ? 'text-[var(--color-primary)] font-semibold' : 'text-[var(--color-outline)]'">
                      {{ u.activo ? 'Activo' : 'Inactivo' }}
                    </span>
                  </td>
                  <td class="px-md py-sm text-right">
                    <div class="flex justify-end gap-xs">
                      <button
                        class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-primary)] hover:bg-[var(--color-surface-container-low)]"
                        type="button"
                        (click)="abrirFormulario(u)"
                      >Editar</button>
                      <button
                        class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-on-surface-variant)] hover:bg-[var(--color-surface-container)]"
                        type="button"
                        (click)="toggleEstado(u)"
                      >{{ u.activo ? 'Desactivar' : 'Activar' }}</button>
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
              {{ usuarioEditando() ? 'Editar usuario' : 'Nuevo usuario' }}
            </h3>
            <form class="space-y-md" (ngSubmit)="guardarUsuario()" #formU="ngForm">
              @if (!usuarioEditando()) {
                <div class="space-y-xs">
                  <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Email *</label>
                  <input class="input-field" type="email" required [(ngModel)]="formUsuario.email" name="email" />
                </div>
              }
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Nombre *</label>
                <input class="input-field" type="text" required [(ngModel)]="formUsuario.nombre" name="nombre" />
              </div>
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Cargo</label>
                <input class="input-field" type="text" [(ngModel)]="formUsuario.cargo" name="cargo" />
              </div>
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Rol *</label>
                <select class="input-field" required [(ngModel)]="formUsuario.rol" name="rol">
                  <option value="CONTRATISTA">Contratista</option>
                  <option value="REVISOR">Revisor</option>
                  <option value="SUPERVISOR">Supervisor</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
              @if (errorForm()) {
                <p class="text-xs text-[var(--color-error)]">{{ errorForm() }}</p>
              }
              <div class="flex justify-end gap-sm pt-sm">
                <button type="button" class="rounded border border-[var(--color-outline-variant)] bg-white px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]" (click)="cerrarFormulario()">Cancelar</button>
                <button type="submit" class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50" [disabled]="guardandoU() || formU.invalid">
                  {{ guardandoU() ? 'Guardando...' : 'Guardar' }}
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
    </style>
  `
})
export class AdminUsuariosComponent implements OnInit {
  readonly pagina = signal<Page<Usuario> | null>(null);
  readonly cargando = signal(true);
  readonly paginaActual = signal(0);
  readonly mostrarFormulario = signal(false);
  readonly usuarioEditando = signal<Usuario | null>(null);
  readonly guardandoU = signal(false);
  readonly errorForm = signal('');

  filtroRol: RolUsuario | '' = '';

  formUsuario: UsuarioRequest = { email: '', nombre: '', cargo: null, rol: 'CONTRATISTA' };

  constructor(private readonly usuarioService: UsuarioService) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.usuarioService.listarUsuarios({ page: this.paginaActual(), size: 15, rol: this.filtroRol || undefined })
      .subscribe({ next: (p) => { this.pagina.set(p); this.cargando.set(false); }, error: () => this.cargando.set(false) });
  }

  cambiarPagina(num: number) { this.paginaActual.set(num); this.cargar(); }

  abrirFormulario(u: Usuario | null) {
    this.usuarioEditando.set(u);
    this.errorForm.set('');
    if (u) {
      this.formUsuario = { email: u.email, nombre: u.nombre, cargo: u.cargo, rol: u.rol };
    } else {
      this.formUsuario = { email: '', nombre: '', cargo: null, rol: 'CONTRATISTA' };
    }
    this.mostrarFormulario.set(true);
  }

  cerrarFormulario() { this.mostrarFormulario.set(false); }

  guardarUsuario() {
    this.guardandoU.set(true);
    this.errorForm.set('');
    const u = this.usuarioEditando();
    const op = u
      ? this.usuarioService.actualizarUsuario(u.id, this.formUsuario)
      : this.usuarioService.crearUsuario(this.formUsuario);
    op.subscribe({
      next: () => { this.guardandoU.set(false); this.cerrarFormulario(); this.cargar(); },
      error: (err) => {
        this.guardandoU.set(false);
        this.errorForm.set(this.mensajeErrorGuardarUsuario(err));
      }
    });
  }

  toggleEstado(u: Usuario) {
    this.usuarioService.cambiarEstado(u.id, !u.activo).subscribe(() => this.cargar());
  }

  private mensajeErrorGuardarUsuario(err: any) {
    const codigo = err?.error?.error ?? '';
    if (err?.status === 0) {
      return 'No se pudo conectar con el backend. Verifica que Spring Boot este iniciado en localhost:8080.';
    }
    if (err?.status === 401 || err?.status === 403) {
      return 'No tienes permisos de administrador o la sesion expiro.';
    }
    if (codigo === 'EMAIL_DUPLICADO') {
      return 'El email ya esta registrado.';
    }
    if (codigo === 'VALIDACION_FALLIDA') {
      return err?.error?.mensaje ?? 'Revisa los campos obligatorios del usuario.';
    }
    return err?.error?.mensaje ?? 'Error al guardar el usuario.';
  }
}
