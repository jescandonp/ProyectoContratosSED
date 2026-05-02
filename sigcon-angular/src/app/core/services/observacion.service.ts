import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { InformeDetalle } from '../models/informe.model';
import { ObservacionRequest } from '../models/observacion.model';

@Injectable({ providedIn: 'root' })
export class ObservacionService {
  private readonly informeBaseUrl = '/api/informes';

  constructor(private readonly http: HttpClient) {}

  aprobarRevision(informeId: number, request: ObservacionRequest | null = null) {
    return this.http.post<InformeDetalle>(`${this.informeBaseUrl}/${informeId}/aprobar-revision`, request);
  }

  devolverRevision(informeId: number, request: ObservacionRequest) {
    return this.http.post<InformeDetalle>(`${this.informeBaseUrl}/${informeId}/devolver-revision`, request);
  }

  devolverInforme(informeId: number, request: ObservacionRequest) {
    return this.http.post<InformeDetalle>(`${this.informeBaseUrl}/${informeId}/devolver`, request);
  }
}
