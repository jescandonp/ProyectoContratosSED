import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { ActividadInforme, ActividadInformeRequest } from '../models/actividad-informe.model';

@Injectable({ providedIn: 'root' })
export class ActividadInformeService {
  constructor(private readonly http: HttpClient) {}

  crear(informeId: number, request: ActividadInformeRequest) {
    return this.http.post<ActividadInforme>(this.baseUrl(informeId), request);
  }

  actualizar(informeId: number, actividadId: number, request: ActividadInformeRequest) {
    return this.http.put<ActividadInforme>(`${this.baseUrl(informeId)}/${actividadId}`, request);
  }

  eliminar(informeId: number, actividadId: number) {
    return this.http.delete<void>(`${this.baseUrl(informeId)}/${actividadId}`);
  }

  private baseUrl(informeId: number) {
    return `/api/informes/${informeId}/actividades`;
  }
}
