import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ContratistaResultado {
  id: number;
  nombre: string;
  email: string;
  cargo: string | null;
}

export interface ContratoResultado {
  id: number;
  numero: string;
  objeto: string;
  estado: string;
  contratistaNombre: string | null;
}

export interface InformeResultado {
  id: number;
  numero: number;
  estado: string;
  fechaInicio: string;
  fechaFin: string;
  contratoNumero: string | null;
  contratistaNombre: string | null;
}

export interface BusquedaAdminResponse {
  contratistas: ContratistaResultado[];
  contratos: ContratoResultado[];
  informes: InformeResultado[];
}

@Injectable({ providedIn: 'root' })
export class BusquedaAdminService {
  constructor(private readonly http: HttpClient) {}

  buscar(q: string, fechaInicio?: string, fechaFin?: string): Observable<BusquedaAdminResponse> {
    let params = new HttpParams().set('q', q);
    if (fechaInicio) params = params.set('fechaInicio', fechaInicio);
    if (fechaFin) params = params.set('fechaFin', fechaFin);
    return this.http.get<BusquedaAdminResponse>('/api/admin/busqueda', { params });
  }
}
