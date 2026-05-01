import { Injectable, signal } from '@angular/core';

import { RolUsuario, Usuario } from '../models/usuario.model';

export interface DevSession {
  id: number;
  email: string;
  nombre: string;
  cargo: string;
  rol: RolUsuario;
  password: string;
}

const STORAGE_KEY = 'sigcon.dev.session';

const DEV_USERS: Record<RolUsuario, DevSession> = {
  ADMIN: {
    id: 1,
    email: 'admin@educacionbogota.edu.co',
    nombre: 'Administrador SIGCON',
    cargo: 'Administrador',
    rol: 'ADMIN',
    password: 'admin123'
  },
  CONTRATISTA: {
    id: 2,
    email: 'juan.escandon@educacionbogota.edu.co',
    nombre: 'Juan Escandon',
    cargo: 'Contratista',
    rol: 'CONTRATISTA',
    password: 'contratista123'
  },
  REVISOR: {
    id: 3,
    email: 'revisor1@educacionbogota.edu.co',
    nombre: 'Revisor SIGCON',
    cargo: 'Revisor',
    rol: 'REVISOR',
    password: 'revisor123'
  },
  SUPERVISOR: {
    id: 4,
    email: 'supervisor1@educacionbogota.edu.co',
    nombre: 'Supervisor SIGCON',
    cargo: 'Supervisor',
    rol: 'SUPERVISOR',
    password: 'supervisor123'
  }
};

@Injectable({ providedIn: 'root' })
export class DevSessionService {
  private readonly session = signal<DevSession | null>(this.loadSession());
  readonly currentSession = this.session.asReadonly();
  readonly users = Object.values(DEV_USERS);

  loginAs(rol: RolUsuario) {
    const session = DEV_USERS[rol];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.session.set(session);
    return session;
  }

  logout() {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }

  authorizationHeader() {
    const session = this.session();
    return session ? `Basic ${btoa(`${session.email}:${session.password}`)}` : null;
  }

  toUsuario(session: DevSession): Usuario {
    return {
      id: session.id,
      email: session.email,
      nombre: session.nombre,
      cargo: session.cargo,
      rol: session.rol,
      firmaImagen: null,
      activo: true
    };
  }

  private loadSession() {
    const rawSession = localStorage.getItem(STORAGE_KEY);
    if (!rawSession) {
      return null;
    }
    try {
      return JSON.parse(rawSession) as DevSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
