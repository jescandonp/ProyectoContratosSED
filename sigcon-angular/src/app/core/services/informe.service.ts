import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { InformeDetalle, InformeRequest, InformeResumen } from '../models/informe.model';
import { Page } from '../models/page.model';

interface ListarInformesParams {
  page?: number;
  size?: number;
  contratoId?: number;
}

@Injectable({ providedIn: 'root' })
export class InformeService {
  private readonly baseUrl = '/api/informes';

  constructor(private readonly http: HttpClient) {}

  listarInformes(params: ListarInformesParams = {}) {
    return this.http.get<Page<InformeResumen>>(this.baseUrl, { params: this.toHttpParams(params) });
  }

  obtenerDetalle(id: number) {
    return this.http.get<InformeDetalle>(`${this.baseUrl}/${id}`);
  }

  crearInforme(request: InformeRequest) {
    return this.http.post<InformeDetalle>(this.baseUrl, request);
  }

  actualizarInforme(id: number, request: InformeRequest) {
    return this.http.put<InformeDetalle>(`${this.baseUrl}/${id}`, request);
  }

  enviarInforme(id: number) {
    return this.http.post<InformeDetalle>(`${this.baseUrl}/${id}/enviar`, null);
  }

  aprobarInforme(id: number) {
    return this.http.post<InformeDetalle>(`${this.baseUrl}/${id}/aprobar`, null);
  }

  private toHttpParams(params: ListarInformesParams) {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }
}
