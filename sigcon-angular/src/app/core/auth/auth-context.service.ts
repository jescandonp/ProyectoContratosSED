import { Injectable, signal } from '@angular/core';

import { RolUsuario, Usuario } from '../models/usuario.model';

export type ActiveRole = 'CONTRATISTA' | 'ADMIN';

@Injectable({ providedIn: 'root' })
export class AuthContextService {
  readonly activeRole = signal<ActiveRole>('CONTRATISTA');

  readonly roleOptions = [
    { label: 'Contratista', value: 'CONTRATISTA' as ActiveRole },
    { label: 'Admin', value: 'ADMIN' as ActiveRole }
  ];

  setActiveRole(role: ActiveRole) {
    this.activeRole.set(role);
  }

  hasRole(user: Usuario | null, roles: RolUsuario[]) {
    if (!user) {
      return false;
    }
    if (this.isDualUser(user)) {
      return roles.includes(this.activeRole());
    }
    return roles.includes(user.rol);
  }

  isDualUser(user: Usuario | null) {
    return user?.rol === 'CONTRATISTA' && user.esAdmin === true;
  }
}
