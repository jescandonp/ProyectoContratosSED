import { computed, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { RolUsuario, Usuario } from '../models/usuario.model';
import { UsuarioService } from '../services/usuario.service';
import { DevSessionService } from './dev-session.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly remoteUser = signal<Usuario | null>(null);

  readonly currentUser = computed(() => {
    const devSession = this.devSession.currentSession();
    if (environment.useDevSession && devSession) {
      return this.devSession.toUsuario(devSession);
    }
    return this.remoteUser();
  });

  constructor(
    private readonly devSession: DevSessionService,
    private readonly usuarioService: UsuarioService,
    private readonly router: Router
  ) {}

  loginDev(rol: RolUsuario) {
    const session = this.devSession.loginAs(rol);
    return this.devSession.toUsuario(session);
  }

  refreshProfile() {
    return this.usuarioService.obtenerPerfilActual().subscribe((usuario) => this.remoteUser.set(usuario));
  }

  logout() {
    this.devSession.logout();
    this.remoteUser.set(null);
    void this.router.navigate(['/login']);
  }

  isAuthenticated() {
    return this.currentUser() !== null;
  }

  hasRole(...roles: RolUsuario[]) {
    const user = this.currentUser();
    return !!user && roles.includes(user.rol);
  }
}
