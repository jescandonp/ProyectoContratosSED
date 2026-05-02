import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DocumentoAdicionalService } from './documento-adicional.service';
import { DocumentoAdicionalRequest } from '../models/documento-adicional.model';

describe('DocumentoAdicionalService', () => {
  let service: DocumentoAdicionalService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DocumentoAdicionalService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses relative I2 documento adicional endpoints nested under informes', () => {
    const request: DocumentoAdicionalRequest = {
      idCatalogo: 30,
      referencia: 'Radicado 123'
    };

    service.agregar(10, request).subscribe();
    const addRequest = http.expectOne('/api/informes/10/documentos-adicionales');
    expect(addRequest.request.method).toBe('POST');
    expect(addRequest.request.body).toEqual(request);
    addRequest.flush(documento());

    service.eliminar(10, 13).subscribe();
    expect(http.expectOne('/api/informes/10/documentos-adicionales/13').request.method).toBe('DELETE');
  });

  function documento() {
    return {
      id: 13,
      idCatalogo: 30,
      nombreCatalogo: 'Certificacion',
      obligatorio: true,
      referencia: 'Radicado 123'
    };
  }
});
