import { Component, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

interface NavItem {
  label: string;
  iconAsset: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="flex h-full w-56 flex-col bg-[#0a0e5a] text-white">

      <!-- Franja logo: fondo blanco -->
      <div class="flex h-[72px] items-center justify-center bg-white px-4">
        <img
          src="assets/images/logo-sigcon.png"
          alt="SIGCON — Una educación que te responde"
          class="h-11 w-auto object-contain"
        />
      </div>

      <!-- Navegación principal -->
      <nav class="flex flex-1 flex-col gap-1 p-2" aria-label="Navegacion principal">
        @for (item of navItems(); track item.route) {
          <a
            class="flex h-10 items-center gap-2 rounded px-2 text-sm font-medium text-white/75 no-underline hover:bg-white/10 hover:text-white transition-colors"
            routerLinkActive="!bg-[#1a2080] !text-white !border-l-[3px] !border-[#e8401c] !pl-[5px]"
            [routerLink]="item.route"
          >
            <img
              [src]="'assets/icons/' + item.iconAsset"
              [alt]="item.label"
              class="h-5 w-5 shrink-0 object-contain brightness-0 invert"
              aria-hidden="true"
            />
            <span>{{ item.label }}</span>
          </a>
        }
      </nav>

      <!-- Sección inferior: perfil y salir -->
      <div class="border-t border-white/15 p-2 flex flex-col gap-1">
        <a
          class="flex h-10 items-center gap-2 rounded px-2 text-sm font-medium text-white/75 no-underline hover:bg-white/10 hover:text-white transition-colors"
          routerLinkActive="!bg-[#1a2080] !text-white !border-l-[3px] !border-[#e8401c] !pl-[5px]"
          [routerLink]="'/perfil'"
        >
          <img src="assets/icons/ico-perfil_.png" alt="Mi Perfil" class="h-5 w-5 shrink-0 object-contain brightness-0 invert" aria-hidden="true" />
          <span>Mi Perfil</span>
        </a>
      </div>
    </aside>
  `
})
export class SidebarComponent {
  readonly navItems = computed<NavItem[]>(() => {
    const items: NavItem[] = [
      { label: 'Contratos', iconAsset: 'ico-contratos_.png', route: '/contratos' }
    ];

    if (this.authService.hasRole('REVISOR')) {
      items.push({ label: 'Revision', iconAsset: 'ico-contratos-admin_.png', route: '/revision/informes' });
    }

    if (this.authService.hasRole('SUPERVISOR')) {
      items.push({ label: 'Aprobacion', iconAsset: 'ico-contratos-admin_.png', route: '/aprobacion/informes' });
    }

    if (this.authService.hasRole('ADMIN') || this.authService.hasRole('ADMINISTRATIVO')) {
      items.push({ label: 'Visto Bueno', iconAsset: 'ico-contratos-admin_.png', route: '/visto-bueno' });
    }

    if (this.authService.hasRole('ADMIN')) {
      items.push(
        { label: 'Administracion', iconAsset: 'ico-admin_.png', route: '/admin' },
        { label: 'Contratos Admin', iconAsset: 'ico-contratos_.png', route: '/admin/contratos' },
        { label: 'Usuarios', iconAsset: 'ico-usuarios_.png', route: '/admin/usuarios' },
        { label: 'Catalogo', iconAsset: 'ico-catalogo_.png', route: '/admin/documentos-catalogo' }
      );
    }

    return items;
  });

  constructor(private readonly authService: AuthService) {}
}
