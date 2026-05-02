import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { Notificacion } from '../models/notificacion.model';
import { NotificacionService } from './notificacion.service';

describe('NotificacionService', () => {
  let service: NotificacionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NotificacionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists notifications using the I3 paginated endpoint', () => {
    service.listar(0, 10).subscribe((page) => {
      expect(page.content[0].tipoEvento).toBe('INFORME_APROBADO');
    });

    const request = http.expectOne((req) =>
      req.url === '/api/notificaciones' &&
      req.params.get('page') === '0' &&
      req.params.get('size') === '10'
    );
    expect(request.request.method).toBe('GET');
    request.flush({
      content: [notificacion()],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true
    });
  });

  it('counts unread notifications and marks notifications as read', () => {
    service.contarNoLeidas().subscribe((response) => expect(response.count).toBe(3));
    const countRequest = http.expectOne('/api/notificaciones/no-leidas/count');
    expect(countRequest.request.method).toBe('GET');
    countRequest.flush({ count: 3 });

    service.marcarLeida(20).subscribe((response) => expect(response.leida).toBeTrue());
    const markRequest = http.expectOne('/api/notificaciones/20/leida');
    expect(markRequest.request.method).toBe('PATCH');
    markRequest.flush({ ...notificacion(), id: 20, leida: true });

    service.marcarTodasLeidas().subscribe();
    expect(http.expectOne('/api/notificaciones/leidas').request.method).toBe('PATCH');
  });

  it('polls unread count every 30 seconds by default', fakeAsync(() => {
    const counts: number[] = [];
    const subscription = service.pollNoLeidas().subscribe((response) => counts.push(response.count));

    tick(0);
    http.expectOne('/api/notificaciones/no-leidas/count').flush({ count: 1 });
    tick(30000);
    http.expectOne('/api/notificaciones/no-leidas/count').flush({ count: 2 });

    expect(counts).toEqual([1, 2]);
    subscription.unsubscribe();
  }));

  function notificacion(): Notificacion {
    return {
      id: 10,
      titulo: 'Informe aprobado',
      descripcion: 'El informe fue aprobado',
      tipoEvento: 'INFORME_APROBADO',
      idInforme: 50,
      leida: false,
      fecha: '2026-05-02T16:00:00'
    };
  }
});
