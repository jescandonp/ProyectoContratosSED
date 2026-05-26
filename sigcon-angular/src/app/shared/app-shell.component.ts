import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { GovcoBarComponent } from './components/govco-bar/govco-bar.component';
import { FooterInstitucionalComponent } from './components/footer/footer-institucional.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopbarComponent } from './components/topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, GovcoBarComponent, FooterInstitucionalComponent, SidebarComponent, TopbarComponent],
  template: `
    <div class="shell-root flex flex-col min-h-screen">
      <app-govco-bar />
      <div class="shell-body flex-1 grid grid-cols-[16rem_1fr] bg-[var(--color-surface)]">
        <app-sidebar />
        <div class="shell-content flex flex-col min-w-0">
          <app-topbar />
          <main class="mx-auto max-w-[1440px] p-lg">
            <router-outlet />
          </main>
        </div>
      </div>
      <app-footer-institucional />
    </div>
  `
})
export class AppShellComponent {}
