import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { ContratoDetalle, ContratoRequest, ContratoResumen, EstadoContrato } from '../models/contrato.model';
import { Page } from '../models/page.model';

interface ListarContratosParams {
  page?: number;
  size?: number;
  estado?: EstadoContrato;
  search?: string;
}

@Injectable({ providedIn: 'root' })
export class ContratoService {
  private readonly baseUrl = '/api/contratos';

  constructor(private readonly http: HttpClient) {}

  listarContratos(params: ListarContratosParams = {}) {
    return this.http.get<Page<ContratoResumen>>(this.baseUrl, { params: this.toHttpParams(params) });
  }

  obtenerDetalle(id: number) {
    return this.http.get<ContratoDetalle>(`${this.baseUrl}/${id}`);
  }

  crearContrato(request: ContratoRequest) {
    return this.http.post<ContratoDetalle>(this.baseUrl, request);
  }

  actualizarContrato(id: number, request: ContratoRequest) {
    return this.http.put<ContratoDetalle>(`${this.baseUrl}/${id}`, request);
  }

  cambiarEstado(id: number, estado: EstadoContrato) {
    return this.http.patch<void>(`${this.baseUrl}/${id}/estado`, { estado });
  }

  eliminarContrato(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private toHttpParams(params: ListarContratosParams) {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }
}
