import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { DocumentoCatalogo, DocumentoCatalogoRequest } from '../models/documento-catalogo.model';
import { Page } from '../models/page.model';
import { TipoContrato } from '../models/contrato.model';

interface ListarDocumentosParams {
  page?: number;
  size?: number;
  tipoContrato?: TipoContrato;
}

@Injectable({ providedIn: 'root' })
export class DocumentoCatalogoService {
  private readonly baseUrl = '/api/documentos-catalogo';

  constructor(private readonly http: HttpClient) {}

  listar(params: ListarDocumentosParams = {}) {
    return this.http.get<Page<DocumentoCatalogo>>(this.baseUrl, { params: this.toHttpParams(params) });
  }

  crear(request: DocumentoCatalogoRequest) {
    return this.http.post<DocumentoCatalogo>(this.baseUrl, request);
  }

  actualizar(id: number, request: DocumentoCatalogoRequest) {
    return this.http.put<DocumentoCatalogo>(`${this.baseUrl}/${id}`, request);
  }

  eliminar(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private toHttpParams(params: ListarDocumentosParams) {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }
}
