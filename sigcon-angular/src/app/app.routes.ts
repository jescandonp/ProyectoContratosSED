import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { AppShellComponent } from './shared/app-shell.component';
import { EmptyStateComponent } from './shared/components/empty-state/empty-state.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: 'login',
    component: EmptyStateComponent,
    title: 'Ingreso'
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'perfil',
        component: EmptyStateComponent,
        title: 'Perfil'
      },
      {
        path: 'contratos',
        component: EmptyStateComponent,
        title: 'Contratos'
      },
      {
        path: 'contratos/:id',
        component: EmptyStateComponent,
        title: 'Detalle contrato'
      },
      {
        path: 'admin',
        component: EmptyStateComponent,
        canActivate: [roleGuard(['ADMIN'])],
        title: 'Admin'
      },
      {
        path: 'admin/contratos',
        component: EmptyStateComponent,
        canActivate: [roleGuard(['ADMIN'])],
        title: 'Contratos admin'
      },
      {
        path: 'admin/contratos/nuevo',
        component: EmptyStateComponent,
        canActivate: [roleGuard(['ADMIN'])],
        title: 'Nuevo contrato'
      },
      {
        path: 'admin/contratos/:id/editar',
        component: EmptyStateComponent,
        canActivate: [roleGuard(['ADMIN'])],
        title: 'Editar contrato'
      },
      {
        path: 'admin/usuarios',
        component: EmptyStateComponent,
        canActivate: [roleGuard(['ADMIN'])],
        title: 'Usuarios'
      },
      {
        path: 'admin/documentos-catalogo',
        component: EmptyStateComponent,
        canActivate: [roleGuard(['ADMIN'])],
        title: 'Catalogo documentos'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
