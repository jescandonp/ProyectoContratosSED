import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

export interface ParametroVb {
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
}
