import { Component, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="flex h-full w-64 flex-col border-r border-[var(--color-outline-variant)] bg-[var(--color-primary)] text-white">
      <div class="border-b border-white/20 px-md py-lg">
        <p class="m-0 text-xs font-bold uppercase tracking-normal text-[var(--color-secondary-container)]">SED Bogota</p>
        <h1 class="m-0 mt-xs text-lg font-bold">SIGCON</h1>
      </div>

      <nav class="flex flex-1 flex-col gap-xs p-sm" aria-label="Navegacion principal">
        @for (item of navItems(); track item.route) {
          <a
            class="flex h-10 items-center gap-sm rounded px-sm text-sm font-semibold text-white/90 no-underline hover:bg-white/10"
            routerLinkActive="bg-white/15 text-white"
            [routerLink]="item.route"
          >
            <i class="pi" [class]="item.icon" aria-hidden="true"></i>
            <span>{{ item.label }}</span>
          </a>
        }
      </nav>
    </aside>
  `
})
export class SidebarComponent {
  readonly navItems = computed<NavItem[]>(() => {
    const items: NavItem[] = [
      { label: 'Perfil', icon: 'pi-user', route: '/perfil' },
      { label: 'Contratos', icon: 'pi-briefcase', route: '/contratos' }
    ];

    if (this.authService.hasRole('ADMIN')) {
      items.push(
        { label: 'Administracion', icon: 'pi-th-large', route: '/admin' },
        { label: 'Contratos admin', icon: 'pi-list-check', route: '/admin/contratos' },
        { label: 'Usuarios', icon: 'pi-users', route: '/admin/usuarios' },
        { label: 'Catalogo', icon: 'pi-file', route: '/admin/documentos-catalogo' }
      );
    }

    return items;
  });

  constructor(private readonly authService: AuthService) {}
}
