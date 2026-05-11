import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { DocumentoRequerido, EmlPreview } from '../models/documento-requerido.model';

@Injectable({ providedIn: 'root' })
export class DocumentoRequeridoService {
  constructor(private readonly http: HttpClient) {}

  /** Lista los documentos requeridos del informe, incluida FACTURA dinámica si aplica. */
  listar(informeId: number): Observable<DocumentoRequerido[]> {
    return this.http.get<DocumentoRequerido[]>(this.baseUrl(informeId));
  }

  /** Carga o reemplaza el archivo de un documento requerido. */
  cargarArchivo(informeId: number, claveLogica: string, archivo: File): Observable<DocumentoRequerido> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<DocumentoRequerido>(
      `${this.baseUrl(informeId)}/${claveLogica}/archivo`,
      formData
    );
  }

  /** Descarga el archivo de un documento requerido como Blob. */
  descargarArchivo(informeId: number, documentoId: number): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl(informeId)}/${documentoId}/archivo`,
      { responseType: 'blob' }
    );
  }

  /** Preview básico de un archivo EML. */
  previewEml(informeId: number, documentoId: number): Observable<EmlPreview> {
    return this.http.get<EmlPreview>(
      `${this.baseUrl(informeId)}/${documentoId}/preview`
    );
  }

  /** Elimina (soft-delete) el archivo de un documento requerido. */
  eliminarArchivo(informeId: number, documentoId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl(informeId)}/${documentoId}/archivo`
    );
  }

  private baseUrl(informeId: number): string {
    return `/api/informes/${informeId}/documentos-requeridos`;
  }
}
