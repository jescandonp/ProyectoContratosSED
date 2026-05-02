import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../core/auth/auth.service';
import { Usuario } from '../../core/models/usuario.model';
import { UsuarioService } from '../../core/services/usuario.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="mx-auto max-w-2xl space-y-lg">

      <!-- Header -->
      <div>
        <h2 class="m-0 text-2xl font-bold text-[var(--color-on-surface)]">Mi Perfil</h2>
        <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">Gestiona tu información personal y firma digital</p>
      </div>

      @if (usuario(); as u) {
        <!-- Profile card -->
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <div class="mb-md flex items-center gap-md border-b border-[var(--color-outline-variant)] pb-md">
            <div class="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-primary-container)] text-lg font-bold text-[var(--color-on-primary)]">
              {{ u.nombre.charAt(0) }}
            </div>
            <div>
              <p class="m-0 font-semibold text-[var(--color-on-surface)]">{{ u.nombre }}</p>
              <p class="m-0 text-sm text-[var(--color-on-surface-variant)]">{{ u.email }}</p>
            </div>
            <span class="ml-auto rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-sm py-xs text-xs font-bold uppercase text-[var(--color-primary)]">
              {{ u.rol }}
            </span>
          </div>

          <!-- Edit form -->
          <form class="space-y-md" (ngSubmit)="guardarPerfil()">
            <div class="grid grid-cols-2 gap-md">
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Nombre</label>
                <input
                  class="h-10 w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none transition-all focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]"
                  type="text"
                  required
                  [(ngModel)]="nombre"
                  name="nombre"
                />
              </div>
              <div class="space-y-xs">
                <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">Cargo</label>
                <input
                  class="h-10 w-full rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-bright)] px-sm text-sm outline-none transition-all focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)]"
                  type="text"
                  [(ngModel)]="cargo"
                  name="cargo"
                />
              </div>
            </div>

            @if (mensajeExito()) {
              <div class="rounded border border-[var(--color-primary-container)] bg-[var(--color-surface-container-low)] px-sm py-xs text-sm text-[var(--color-primary)]">
                {{ mensajeExito() }}
              </div>
            }

            <div class="flex justify-end">
              <button
                class="rounded bg-[var(--color-primary)] px-lg py-sm text-sm font-semibold text-white transition-all hover:opacity-90 disabled:opacity-50"
                type="submit"
                [disabled]="guardando()"
              >
                {{ guardando() ? 'Guardando...' : 'Guardar cambios' }}
              </button>
            </div>
          </form>
        </div>

        <!-- Firma card -->
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
          <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Firma Digital</h3>

          @if (u.rol === 'CONTRATISTA' || u.rol === 'SUPERVISOR') {
            <p class="mb-md text-sm text-[var(--color-on-surface-variant)]">
              La firma se usará para firmar los informes aprobados. Sube una imagen JPG o PNG (máx. 2 MB).
            </p>
            @if (!u.firmaImagen) {
              <div
                class="mb-md rounded border border-[var(--color-secondary-container)] bg-[var(--color-secondary-fixed)] px-sm py-xs text-sm font-semibold text-[var(--color-secondary)]"
                data-testid="firma-warning"
              >
                Para que los informes puedan ser aprobados, debe cargar su firma digital.
              </div>
            }
          }

          <!-- Current signature preview -->
          @if (u.firmaImagen) {
            <div class="mb-md rounded border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-sm">
              <p class="mb-xs text-xs font-bold uppercase text-[var(--color-on-surface-variant)]">Firma actual</p>
              <img
                class="h-24 w-auto object-contain"
                [src]="'/api/storage/' + u.firmaImagen"
                alt="Firma actual"
              />
            </div>
          }

          <!-- Upload -->
          <div class="space-y-xs">
            <label class="text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
              Subir nueva firma (JPG / PNG)
            </label>
            <input
              class="block w-full text-sm text-[var(--color-on-surface-variant)] file:mr-sm file:rounded file:border file:border-[var(--color-outline-variant)] file:bg-[var(--color-surface-container-low)] file:px-sm file:py-xs file:text-xs file:font-semibold"
              type="file"
              accept=".jpg,.jpeg,.png"
              (change)="onFirmaSelected($event)"
            />
            @if (errorFirma()) {
              <p class="text-xs text-[var(--color-error)]">{{ errorFirma() }}</p>
            }
            @if (firmaFile()) {
              <button
                class="rounded bg-[var(--color-primary)] px-md py-xs text-sm font-semibold text-white transition-all hover:opacity-90 disabled:opacity-50"
                type="button"
                [disabled]="subiendoFirma()"
                (click)="subirFirma()"
              >
                {{ subiendoFirma() ? 'Subiendo...' : 'Guardar firma' }}
              </button>
            }
          </div>
        </div>
      } @else {
        <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-xl text-center text-sm text-[var(--color-on-surface-variant)]">
          Cargando perfil...
        </div>
      }
    </div>
  `
})
export class PerfilComponent implements OnInit {
  readonly usuario = signal<Usuario | null>(null);
  readonly guardando = signal(false);
  readonly mensajeExito = signal('');
  readonly subiendoFirma = signal(false);
  readonly errorFirma = signal('');
  readonly firmaFile = signal<File | null>(null);

  nombre = '';
  cargo = '';

  constructor(
    private readonly usuarioService: UsuarioService,
    private readonly authService: AuthService
  ) {}

  ngOnInit() {
    const current = this.authService.currentUser();
    if (current) {
      this.usuario.set(current);
      this.nombre = current.nombre;
      this.cargo = current.cargo ?? '';
    }
    this.usuarioService.obtenerPerfilActual().subscribe((u) => {
      this.usuario.set(u);
      this.nombre = u.nombre;
      this.cargo = u.cargo ?? '';
    });
  }

  guardarPerfil() {
    if (!this.nombre.trim()) return;
    this.guardando.set(true);
    this.mensajeExito.set('');
    this.usuarioService.actualizarPerfilActual({ nombre: this.nombre, cargo: this.cargo || null })
      .subscribe({
        next: (u) => {
          this.usuario.set(u);
          this.mensajeExito.set('Perfil actualizado correctamente.');
          this.guardando.set(false);
        },
        error: () => {
          this.guardando.set(false);
        }
      });
  }

  onFirmaSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.errorFirma.set('');
    this.firmaFile.set(null);
    if (!file) return;
    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
      this.errorFirma.set('Solo se aceptan archivos JPG o PNG.');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.errorFirma.set('El archivo no puede superar 2 MB.');
      return;
    }
    this.firmaFile.set(file);
  }

  subirFirma() {
    const file = this.firmaFile();
    if (!file) return;
    this.subiendoFirma.set(true);
    this.errorFirma.set('');
    this.usuarioService.actualizarFirmaActual(file).subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.firmaFile.set(null);
        this.subiendoFirma.set(false);
        this.mensajeExito.set('Firma actualizada correctamente.');
      },
      error: () => {
        this.errorFirma.set('Error al subir la firma. Intente de nuevo.');
        this.subiendoFirma.set(false);
      }
    });
  }
}
