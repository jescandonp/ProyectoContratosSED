import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { RolUsuario } from '../models/usuario.model';
import { AuthService } from './auth.service';

export function roleGuard(roles: RolUsuario[]): CanActivateFn {
  return (_route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    return authService.hasRole(...roles) ? true : router.createUrlTree(['/contratos']);
  };
}
