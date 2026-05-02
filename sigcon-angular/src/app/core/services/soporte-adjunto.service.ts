import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { SoporteAdjunto, SoporteUrlRequest } from '../models/soporte-adjunto.model';

@Injectable({ providedIn: 'root' })
export class SoporteAdjuntoService {
  constructor(private readonly http: HttpClient) {}

  agregarUrl(actividadId: number, request: SoporteUrlRequest) {
    return this.http.post<SoporteAdjunto>(`${this.baseUrl(actividadId)}/url`, request);
  }

  agregarArchivo(actividadId: number, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<SoporteAdjunto>(`${this.baseUrl(actividadId)}/archivo`, formData);
  }

  eliminar(actividadId: number, soporteId: number) {
    return this.http.delete<void>(`${this.baseUrl(actividadId)}/${soporteId}`);
  }

  private baseUrl(actividadId: number) {
    return `/api/actividades/${actividadId}/soportes`;
  }
}
