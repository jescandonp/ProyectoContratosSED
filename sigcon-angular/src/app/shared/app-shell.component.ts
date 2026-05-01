import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SidebarComponent } from './components/sidebar/sidebar.component';
import { TopbarComponent } from './components/topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  template: `
    <div class="grid min-h-screen grid-cols-[16rem_1fr] bg-[var(--color-surface)]">
      <app-sidebar />
      <div class="min-w-0">
        <app-topbar />
        <main class="mx-auto max-w-[1440px] p-lg">
          <router-outlet />
        </main>
      </div>
    </div>
  `
})
export class AppShellComponent {}
