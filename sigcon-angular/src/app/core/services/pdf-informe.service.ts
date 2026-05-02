import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PdfInformeService {
  constructor(private readonly http: HttpClient) {}

  descargar(idInforme: number): Observable<Blob> {
    return this.http.get(`/api/informes/${idInforme}/pdf`, { responseType: 'blob' });
  }
}
