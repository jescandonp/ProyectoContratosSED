import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { AuthService } from '../../core/auth/auth.service';
import { RolUsuario } from '../../core/models/usuario.model';
import { GovcoBarComponent } from '../../shared/components/govco-bar/govco-bar.component';
import { FooterInstitucionalComponent } from '../../shared/components/footer/footer-institucional.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ToastModule, GovcoBarComponent, FooterInstitucionalComponent],
  providers: [MessageService],
  template: `
    <p-toast></p-toast>
    <div class="login-page">
      <app-govco-bar />

      <!-- Header with brand logo -->
      <header class="login-header">
        <div class="brand-left">
          <img
            src="assets/images/logo-head-sigcon.png"
            alt="SIGCON"
            class="brand-logo"
          />
        </div>
        <div class="inst-logos-placeholder">
          <!-- logos SED/Alcaldía/Bogotá — pendiente comunicaciones -->
        </div>
      </header>

      <!-- Main content with background image -->
      <main class="login-main">
        <div class="login-card">
          <!-- Card header -->
          <div class="card-header">
            <h2>BIENVENIDO</h2>
            <p class="subtitle">Acceda a la plataforma utilizando sus credenciales institucionales.</p>
          </div>

          <!-- Card body -->
          <div class="card-body">
            <!-- SSO Button -->
            <button
              class="btn-sso"
              type="button"
            >
              <svg class="office-icon" viewBox="0 0 23 23" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="1" y="1" width="10" height="10" fill="#f25022"/>
                <rect x="12" y="1" width="10" height="10" fill="#7fba00"/>
                <rect x="1" y="12" width="10" height="10" fill="#00a4ef"/>
                <rect x="12" y="12" width="10" height="10" fill="#ffb900"/>
              </svg>
              <span>Iniciar con office 365</span>
            </button>

            <!-- Dev Grid -->
            <div class="dev-grid">
              @for (user of devUsers; track user.email) {
                <button
                  class="dev-btn"
                  type="button"
                  (click)="loginDev(user)"
                >
                  <span class="dev-label">{{ user.label }}</span>
                  <span class="dev-email">{{ user.email }}</span>
                </button>
              }
            </div>

            <!-- Forgot password link -->
            <a href="#" class="forgot-link" (click)="onForgotPassword($event)">
              Olvide la contraseña
            </a>
          </div>

          <!-- Version badge -->
          <span class="version-badge">VS {{ version }}</span>
        </div>
      </main>

      <app-footer-institucional />
    </div>
  `,
  styles: [`
    .login-page {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
      font-family: var(--font-family);
    }

    .login-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 32px;
      background: linear-gradient(135deg, #00005f 0%, #1a2080 100%);
      z-index: 10;
      position: relative;
    }

    .brand-left {
      display: flex;
      align-items: center;
    }

    .brand-logo {
      height: 44px;
      width: auto;
      object-fit: contain;
    }

    .inst-logos-placeholder {
      opacity: 0;
      pointer-events: none;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .login-main {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background-image: url('assets/images/ima-fondo.png');
      background-size: cover;
      background-position: center;
      background-attachment: fixed;
      position: relative;
    }

    .login-main::before {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(to bottom, rgba(0, 0, 95, 0.55), rgba(0, 0, 95, 0.55));
      z-index: 1;
    }

    .login-card {
      position: relative;
      z-index: 2;
      background: white;
      border-radius: 8px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
      max-width: 420px;
      width: 100%;
      overflow: hidden;
    }

    .card-header {
      padding: 24px;
      border-bottom: 1px solid #e0e0e0;
    }

    .card-header h2 {
      font-family: var(--font-family-heading);
      font-size: 20px;
      font-weight: bold;
      margin: 0 0 8px 0;
      color: #00005f;
    }

    .subtitle {
      font-size: 14px;
      color: #666;
      margin: 0;
      line-height: 1.5;
    }

    .card-body {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .btn-sso {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      background-color: #f95000;
      color: white;
      border: none;
      border-radius: 4px;
      padding: 12px 16px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: background-color 0.3s;
      font-family: var(--font-family);
    }

    .btn-sso:hover {
      background-color: #d94700;
    }

    .office-icon {
      width: 20px;
      height: 20px;
    }

    .dev-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }

    .dev-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      padding: 12px 8px;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      background: #f9f9f9;
      cursor: pointer;
      transition: all 0.2s;
      font-family: var(--font-family);
    }

    .dev-btn:hover {
      border-color: #f95000;
      background: #fffaf7;
    }

    .dev-label {
      font-size: 12px;
      font-weight: 600;
      color: #f95000;
    }

    .dev-email {
      font-size: 10px;
      color: #999;
      text-align: center;
      word-break: break-word;
    }

    .forgot-link {
      text-align: center;
      font-size: 13px;
      color: #f95000;
      text-decoration: none;
      cursor: pointer;
      transition: color 0.2s;
    }

    .forgot-link:hover {
      color: #d94700;
      text-decoration: underline;
    }

    .version-badge {
      position: absolute;
      bottom: 8px;
      right: 8px;
      font-size: 11px;
      color: #999;
      font-family: var(--font-family);
    }
  `]
})
export class LoginComponent {
  readonly version = '1.0.3';

  readonly devUsers: { rol: RolUsuario; label: string; email: string; useEmail?: boolean }[] = [
    { rol: 'ADMIN', label: 'Admin', email: 'admin@educacionbogota.edu.co' },
    { rol: 'CONTRATISTA', label: 'Contratista', email: 'juan.escandon@...' },
    { rol: 'CONTRATISTA', label: 'Contratista IVA', email: 'aecheverry@educacionbogota.gov.co', useEmail: true },
    { rol: 'REVISOR', label: 'Revisor', email: 'revisor1@...' },
    { rol: 'SUPERVISOR', label: 'Supervisor', email: 'supervisor1@...' }
  ];

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  loginDev(user: { rol: RolUsuario; email: string; useEmail?: boolean }) {
    if (user.useEmail) {
      this.authService.loginDevEmail(user.email);
    } else {
      this.authService.loginDev(user.rol);
    }
    if (user.rol === 'ADMIN') {
      void this.router.navigate(['/admin']);
    } else {
      void this.router.navigate(['/contratos']);
    }
  }

  onForgotPassword(event: Event): void {
    event.preventDefault();
    this.messageService.add({
      severity: 'info',
      summary: 'Recuperación de Contraseña',
      detail: 'Para recuperar su contraseña, contacte a Mesa de Ayuda.'
    });
  }
}
