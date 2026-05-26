import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-govco-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="govco-bar">
      <div class="govco-content">
        <div class="govco-left">
          <div class="orange-dot"></div>
          <span class="govco-text">GOV.CO</span>
          <img
            src="assets/images/logo-govco-blanco.png"
            alt="GOV.CO Logo"
            class="govco-logo"
            (error)="onLogoError()"
            [hidden]="logoFailed"
          />
        </div>
        <span class="colombia-text">Colombia</span>
      </div>
    </div>
  `,
  styles: [`
    .govco-bar {
      position: relative;
      z-index: 100;
      height: 28px;
      background-color: #00005f;
      display: flex;
      align-items: center;
      padding: 0 16px;
    }

    .govco-content {
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .govco-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .orange-dot {
      width: 7px;
      height: 7px;
      background-color: #f95000;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .govco-text {
      color: white;
      font-family: 'Work Sans', sans-serif;
      font-weight: 600;
      font-size: 11px;
      letter-spacing: 1.5px;
      white-space: nowrap;
    }

    .govco-logo {
      height: 16px;
      width: auto;
      object-fit: contain;
    }

    .colombia-text {
      color: white;
      font-family: 'Work Sans', sans-serif;
      font-weight: 400;
      font-size: 10px;
      white-space: nowrap;
    }
  `]
})
export class GovcoBarComponent {
  logoFailed = false;

  onLogoError(): void {
    this.logoFailed = true;
  }
}
