import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

export interface ParametroVb {
  activo: boolean;
}

export interface ParametroCargaInformes {
  activo: boolean;
}

@Injectable({ providedIn: 'root' })
export class ParametroService {
  private readonly baseUrl = '/api/admin/parametros';

  constructor(private readonly http: HttpClient) {}

  obtenerVb() {
    return this.http.get<ParametroVb>(this.baseUrl);
  }

  setVbActivo(activo: boolean) {
    return this.http.put<ParametroVb>(`${this.baseUrl}/vb-activo`, { activo });
  }

  obtenerCargaInformes() {
    return this.http.get<ParametroCargaInformes>(`${this.baseUrl}/carga-informes`);
  }

  setCargaInformesActiva(activo: boolean) {
    return this.http.put<ParametroCargaInformes>(`${this.baseUrl}/carga-informes`, { activo });
  }
}
