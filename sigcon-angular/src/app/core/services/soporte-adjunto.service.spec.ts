import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SoporteAdjuntoService } from './soporte-adjunto.service';
import { SoporteUrlRequest } from '../models/soporte-adjunto.model';

describe('SoporteAdjuntoService', () => {
  let service: SoporteAdjuntoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(SoporteAdjuntoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses relative I2 soporte endpoints and multipart upload', () => {
    const request: SoporteUrlRequest = {
      nombre: 'Evidencia',
      url: 'https://sed.gov.co/evidencia'
    };

    service.agregarUrl(11, request).subscribe();
    const urlRequest = http.expectOne('/api/actividades/11/soportes/url');
    expect(urlRequest.request.method).toBe('POST');
    expect(urlRequest.request.body).toEqual(request);
    urlRequest.flush(soporte());

    const file = new File(['pdf'], 'evidencia.pdf', { type: 'application/pdf' });
    service.agregarArchivo(11, file).subscribe();
    const fileRequest = http.expectOne('/api/actividades/11/soportes/archivo');
    expect(fileRequest.request.method).toBe('POST');
    expect(fileRequest.request.body instanceof FormData).toBeTrue();
    fileRequest.flush(soporte());

    service.eliminar(11, 12).subscribe();
    expect(http.expectOne('/api/actividades/11/soportes/12').request.method).toBe('DELETE');
  });

  function soporte() {
    return {
      id: 12,
      tipo: 'URL',
      nombre: 'Evidencia',
      referencia: 'https://sed.gov.co/evidencia'
    };
  }
});
