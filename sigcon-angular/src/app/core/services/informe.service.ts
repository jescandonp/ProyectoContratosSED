import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { InformeDetalle, InformeRequest, InformeResumen, InformeUpdateDto } from '../models/informe.model';
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

  actualizarPeriodo(id: number, dto: InformeUpdateDto) {
    return this.http.patch<InformeDetalle>(`${this.baseUrl}/${id}`, dto);
  }

  actualizarPorcentajeEjecucion(id: number, porcentajeEjecucion: number) {
    return this.http.patch<InformeDetalle>(`${this.baseUrl}/${id}/porcentaje-ejecucion`, { porcentajeEjecucion });
  }

  listarColaVistoBueno(page = 0, size = 10) {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<Page<InformeResumen>>(`${this.baseUrl}/cola/visto-bueno`, { params });
  }

  darVistosBueno(id: number, observacion?: string) {
    return this.http.post<InformeDetalle>(`${this.baseUrl}/${id}/dar-visto-bueno`, { observacion: observacion ?? null });
  }

  escalar(id: number, observacion?: string) {
    return this.http.post<InformeDetalle>(`${this.baseUrl}/${id}/escalar`, { observacion: observacion ?? null });
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
