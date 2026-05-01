import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { RolUsuario } from '../../core/models/usuario.model';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <div class="relative min-h-screen overflow-hidden bg-[var(--color-background)] flex flex-col font-[Public_Sans,sans-serif]">
      <!-- Background decorators -->
      <div class="pointer-events-none absolute inset-0 opacity-[0.03]"
           style="background-image: radial-gradient(#002869 0.5px, transparent 0.5px), radial-gradient(#002869 0.5px, #f8f9ff 0.5px); background-size: 20px 20px; background-position: 0 0, 10px 10px;"></div>
      <div class="pointer-events-none absolute right-0 top-0 h-1/3 w-1/3 -translate-y-1/2 translate-x-1/2 rounded-full bg-[var(--color-primary)]/5 blur-3xl"></div>
      <div class="pointer-events-none absolute bottom-0 left-0 h-1/4 w-1/4 translate-y-1/2 -translate-x-1/2 rounded-full bg-[var(--color-secondary)]/5 blur-3xl"></div>

      <!-- Main -->
      <main class="relative z-10 flex flex-1 items-center justify-center p-md">
        <div class="flex w-full max-w-[480px] flex-col overflow-hidden rounded-xl border border-[var(--color-outline-variant)] bg-white shadow-lg">

          <!-- Header -->
          <div class="flex flex-col items-center space-y-md border-b border-[var(--color-surface-container)] p-xl text-center">
            <div class="mb-sm flex h-16 w-16 items-center justify-center rounded-xl bg-[var(--color-primary)]">
              <span class="text-3xl font-black text-white">S</span>
            </div>
            <div>
              <h1 class="text-h2 font-h2 tracking-tight text-[var(--color-primary)]">SIGCON</h1>
              <p class="text-label-bold font-label-bold uppercase tracking-[0.15em] text-[var(--color-on-surface-variant)]">
                Sistema de Gestión de Contratos
              </p>
            </div>
          </div>

          <!-- Body -->
          <div class="space-y-lg bg-[var(--color-surface-container-lowest)] p-xl">
            <p class="px-sm text-center text-body-md font-body-md text-[var(--color-on-surface-variant)]">
              Acceda a la plataforma utilizando sus credenciales institucionales de la Secretaría de Educación del Distrito.
            </p>

            <!-- SSO button (placeholder for weblogic profile) -->
            <button
              class="flex w-full items-center justify-center gap-md rounded-xl bg-[var(--color-primary)] px-lg py-md text-body-lg font-body-lg font-semibold text-white shadow-md transition-all duration-200 hover:opacity-90 active:scale-[0.98]"
              type="button"
            >
              <svg class="h-5 w-5" viewBox="0 0 23 23" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="1" y="1" width="10" height="10" fill="#f25022"/>
                <rect x="12" y="1" width="10" height="10" fill="#7fba00"/>
                <rect x="1" y="12" width="10" height="10" fill="#00a4ef"/>
                <rect x="12" y="12" width="10" height="10" fill="#ffb900"/>
              </svg>
              <span>Iniciar sesión con Office 365</span>
            </button>

            <!-- Divider -->
            <div class="relative flex items-center py-md">
              <div class="flex-grow border-t border-[var(--color-outline-variant)]"></div>
              <span class="mx-md flex-shrink text-label-bold font-label-bold uppercase tracking-wider text-[var(--color-outline)]">
                Acceso local (desarrollo)
              </span>
              <div class="flex-grow border-t border-[var(--color-outline-variant)]"></div>
            </div>

            <!-- Dev login buttons -->
            <div class="grid grid-cols-2 gap-sm">
              @for (user of devUsers; track user.rol) {
                <button
                  class="flex flex-col items-center gap-xs rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] px-md py-sm text-left transition-all hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-container)]"
                  type="button"
                  (click)="loginDev(user.rol)"
                >
                  <span class="text-body-sm font-semibold text-[var(--color-primary)]">{{ user.label }}</span>
                  <span class="text-[11px] text-[var(--color-outline)]">{{ user.email }}</span>
                </button>
              }
            </div>

            <!-- Help -->
            <div class="flex items-center justify-center gap-xs rounded-full bg-[var(--color-surface-container-low)] px-md py-sm text-[var(--color-on-surface-variant)]">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>
              </svg>
              <p class="text-body-sm font-body-sm">¿Problemas para ingresar? Contacte a Mesa de Ayuda.</p>
            </div>
          </div>

          <!-- Bottom accent -->
          <div class="h-1.5 w-full bg-gradient-to-r from-[var(--color-primary)] via-[var(--color-tertiary-container)] to-[var(--color-secondary-container)]"></div>
        </div>
      </main>

      <!-- Footer -->
      <footer class="relative z-10 w-full border-t border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-lg">
        <div class="mx-auto flex max-w-7xl flex-col items-center justify-between gap-md md:flex-row">
          <div class="flex flex-col items-center space-y-xs md:items-start">
            <span class="text-label-bold font-label-bold uppercase text-[var(--color-primary)]">Secretaría de Educación del Distrito</span>
            <p class="text-body-sm font-body-sm text-[var(--color-on-surface-variant)]">© 2026 Bogotá D.C. — Todos los derechos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  `
})
export class LoginComponent {
  readonly devUsers: { rol: RolUsuario; label: string; email: string }[] = [
    { rol: 'ADMIN', label: 'Admin', email: 'admin@educacionbogota.edu.co' },
    { rol: 'CONTRATISTA', label: 'Contratista', email: 'juan.escandon@...' },
    { rol: 'REVISOR', label: 'Revisor', email: 'revisor1@...' },
    { rol: 'SUPERVISOR', label: 'Supervisor', email: 'supervisor1@...' }
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  loginDev(rol: RolUsuario) {
    this.authService.loginDev(rol);
    if (rol === 'ADMIN') {
      void this.router.navigate(['/admin']);
    } else {
      void this.router.navigate(['/contratos']);
    }
  }
}
