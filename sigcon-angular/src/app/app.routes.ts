import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { AppShellComponent } from './shared/app-shell.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
    title: 'Ingreso — SIGCON'
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'perfil',
        loadComponent: () => import('./features/perfil/perfil.component').then((m) => m.PerfilComponent),
        title: 'Mi Perfil — SIGCON'
      },
      {
        path: 'contratos',
        loadComponent: () =>
          import('./features/contratos/lista/contratos-lista.component').then((m) => m.ContratosListaComponent),
        title: 'Contratos — SIGCON'
      },
      {
        path: 'contratos/:id',
        loadComponent: () =>
          import('./features/contratos/detalle/contrato-detalle.component').then((m) => m.ContratoDetalleComponent),
        title: 'Detalle de contrato — SIGCON'
      },
      {
        path: 'contratos/:contratoId/informes/nuevo',
        canActivate: [roleGuard(['CONTRATISTA'])],
        loadComponent: () =>
          import('./features/informes/nuevo/informe-form.component').then((m) => m.InformeFormComponent),
        title: 'Nuevo informe — SIGCON'
      },
      {
        path: 'informes/:id',
        loadComponent: () =>
          import('./features/informes/detalle/informe-detalle.component').then((m) => m.InformeDetalleComponent),
        title: 'Detalle de informe — SIGCON'
      },
      {
        path: 'informes/:id/preview',
        loadComponent: () =>
          import('./features/informes/preview/informe-preview.component').then((m) => m.InformePreviewComponent),
        title: 'Vista previa de informe — SIGCON'
      },
      {
        path: 'admin',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
        title: 'Administración — SIGCON'
      },
      {
        path: 'admin/contratos',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/contratos/admin-contratos.component').then((m) => m.AdminContratosComponent),
        title: 'Contratos — Admin SIGCON'
      },
      {
        path: 'admin/contratos/nuevo',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/contratos/admin-contrato-form.component').then((m) => m.AdminContratoFormComponent),
        title: 'Nuevo contrato — Admin SIGCON'
      },
      {
        path: 'admin/contratos/:id/editar',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/contratos/admin-contrato-form.component').then((m) => m.AdminContratoFormComponent),
        title: 'Editar contrato — Admin SIGCON'
      },
      {
        path: 'admin/usuarios',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/usuarios/admin-usuarios.component').then((m) => m.AdminUsuariosComponent),
        title: 'Usuarios — Admin SIGCON'
      },
      {
        path: 'admin/documentos-catalogo',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/catalogo/admin-catalogo.component').then((m) => m.AdminCatalogoComponent),
        title: 'Catálogo — Admin SIGCON'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
