import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, switchMap, timer } from 'rxjs';

import { Notificacion, NotificacionesCount } from '../models/notificacion.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly baseUrl = '/api/notificaciones';

  constructor(private readonly http: HttpClient) {}

  listar(page = 0, size = 10): Observable<Page<Notificacion>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<Page<Notificacion>>(this.baseUrl, { params });
  }

  contarNoLeidas(): Observable<NotificacionesCount> {
    return this.http.get<NotificacionesCount>(`${this.baseUrl}/no-leidas/count`);
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.patch<Notificacion>(`${this.baseUrl}/${id}/leida`, null);
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/leidas`, null);
  }

  pollNoLeidas(intervalMs = 30000): Observable<NotificacionesCount> {
    return timer(0, intervalMs).pipe(switchMap(() => this.contarNoLeidas()));
  }
}
