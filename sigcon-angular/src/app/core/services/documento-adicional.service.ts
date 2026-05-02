import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { DocumentoAdicional, DocumentoAdicionalRequest } from '../models/documento-adicional.model';

@Injectable({ providedIn: 'root' })
export class DocumentoAdicionalService {
  constructor(private readonly http: HttpClient) {}

  agregar(informeId: number, request: DocumentoAdicionalRequest) {
    return this.http.post<DocumentoAdicional>(this.baseUrl(informeId), request);
  }

  eliminar(informeId: number, documentoId: number) {
    return this.http.delete<void>(`${this.baseUrl(informeId)}/${documentoId}`);
  }

  private baseUrl(informeId: number) {
    return `/api/informes/${informeId}/documentos-adicionales`;
  }
}
