import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer-institucional',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="footer-institucional">
      <div class="footer-content">
        <div class="footer-left">
          <div class="footer-section">
            <h3 class="footer-label">Contáctenos</h3>
            <p class="footer-text">Tel: (601) 324 1000</p>
            <p class="footer-text">contactenos@educacionbogota.edu.co</p>
            <p class="footer-text footer-address">Av. El Dorado No 66-63, Bogotá - Colombia</p>
          </div>
        </div>
        <div class="footer-right">
          <div class="footer-logos">
            <img
              src="assets/images/logo-govco-blanco.png"
              alt="GOV.CO Logo"
              class="footer-logo"
              (error)="onLogoError('govco')"
              [hidden]="logoFailed.govco"
            />
            <span class="logo-fallback" *ngIf="logoFailed.govco">gov.co</span>
          </div>
          <div class="footer-institution">
            <p class="institution-text">Secretaría de</p>
            <p class="institution-text">Educación del</p>
            <p class="institution-text">Distrito</p>
          </div>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .footer-institucional {
      background-color: #00005f;
      color: white;
      padding: 16px 32px;
    }

    .footer-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 32px;
    }

    .footer-left {
      flex: 1;
      min-width: 300px;
    }

    .footer-section {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .footer-label {
      font-family: 'Work Sans', sans-serif;
      font-weight: 600;
      font-size: 12px;
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .footer-text {
      font-family: 'Work Sans', sans-serif;
      font-weight: 400;
      font-size: 11px;
      line-height: 1.8;
      margin: 0;
    }

    .footer-address {
      margin-top: 8px;
    }

    .footer-right {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-shrink: 0;
    }

    .footer-logos {
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 50px;
      min-height: 50px;
    }

    .footer-logo {
      max-height: 50px;
      width: auto;
      object-fit: contain;
    }

    .logo-fallback {
      font-family: 'Work Sans', sans-serif;
      font-weight: 400;
      font-size: 11px;
      color: white;
    }

    .footer-institution {
      text-align: right;
    }

    .institution-text {
      font-family: 'Work Sans', sans-serif;
      font-weight: 400;
      font-size: 11px;
      line-height: 1.4;
      margin: 0;
    }

    @media (max-width: 768px) {
      .footer-content {
        flex-direction: column;
        gap: 16px;
      }

      .footer-right {
        width: 100%;
        justify-content: flex-start;
      }

      .footer-institution {
        text-align: left;
      }
    }
  `]
})
export class FooterInstitucionalComponent {
  logoFailed = { govco: false };

  onLogoError(logo: string): void {
    if (logo === 'govco') {
      this.logoFailed.govco = true;
    }
  }
}
