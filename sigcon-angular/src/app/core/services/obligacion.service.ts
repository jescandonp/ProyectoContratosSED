import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Obligacion, ObligacionRequest } from '../models/obligacion.model';

@Injectable({ providedIn: 'root' })
export class ObligacionService {
  constructor(private readonly http: HttpClient) {}

  listarPorContrato(contratoId: number) {
    return this.http.get<Obligacion[]>(this.baseUrl(contratoId));
  }

  crear(contratoId: number, request: ObligacionRequest) {
    return this.http.post<Obligacion>(this.baseUrl(contratoId), request);
  }

  actualizar(contratoId: number, id: number, request: ObligacionRequest) {
    return this.http.put<Obligacion>(`${this.baseUrl(contratoId)}/${id}`, request);
  }

  eliminar(contratoId: number, id: number) {
    return this.http.delete<void>(`${this.baseUrl(contratoId)}/${id}`);
  }

  private baseUrl(contratoId: number) {
    return `/api/contratos/${contratoId}/obligaciones`;
  }
}
