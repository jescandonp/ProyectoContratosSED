import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Page } from '../models/page.model';
import { EstadoUsuarioRequest, PerfilUpdateRequest, RolUsuario, Usuario, UsuarioRequest } from '../models/usuario.model';

interface ListarUsuariosParams {
  page?: number;
  size?: number;
  rol?: RolUsuario;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly baseUrl = '/api/usuarios';

  constructor(private readonly http: HttpClient) {}

  obtenerPerfilActual() {
    return this.http.get<Usuario>(`${this.baseUrl}/me`);
  }

  actualizarPerfilActual(request: PerfilUpdateRequest) {
    return this.http.put<Usuario>(`${this.baseUrl}/me`, request);
  }

  actualizarFirmaActual(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Usuario>(`${this.baseUrl}/me/firma`, formData);
  }

  listarUsuarios(params: ListarUsuariosParams = {}) {
    return this.http.get<Page<Usuario>>(this.baseUrl, { params: this.toHttpParams(params) });
  }

  crearUsuario(request: UsuarioRequest) {
    return this.http.post<Usuario>(this.baseUrl, request);
  }

  actualizarUsuario(id: number, request: UsuarioRequest) {
    return this.http.put<Usuario>(`${this.baseUrl}/${id}`, request);
  }

  cambiarEstado(id: number, activo: boolean) {
    const request: EstadoUsuarioRequest = { activo };
    return this.http.patch<void>(`${this.baseUrl}/${id}/estado`, request);
  }

  private toHttpParams(params: ListarUsuariosParams) {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }
}
