import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { InformeService } from './informe.service';
import { InformeRequest } from '../models/informe.model';

describe('InformeService', () => {
  let service: InformeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(InformeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses relative I2 informe CRUD and transition endpoints', () => {
    service.listarInformes({ page: 0, size: 10, contratoId: 20 }).subscribe();
    const listRequest = http.expectOne((request) =>
      request.url === '/api/informes' &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '10' &&
      request.params.get('contratoId') === '20'
    );
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0, first: true, last: true });

    service.obtenerDetalle(10).subscribe();
    expect(http.expectOne('/api/informes/10').request.method).toBe('GET');

    const request = informeRequest();
    service.crearInforme(request).subscribe();
    const createRequest = http.expectOne('/api/informes');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(request);
    createRequest.flush(informeDetalle());

    service.actualizarInforme(10, request).subscribe();
    const updateRequest = http.expectOne('/api/informes/10');
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(request);
    updateRequest.flush(informeDetalle());

    service.enviarInforme(10).subscribe();
    expect(http.expectOne('/api/informes/10/enviar').request.method).toBe('POST');

    service.aprobarInforme(10).subscribe();
    expect(http.expectOne('/api/informes/10/aprobar').request.method).toBe('POST');
  });

  function informeRequest(): InformeRequest {
    return {
      idContrato: 20,
      fechaInicio: '2026-02-01',
      fechaFin: '2026-02-28'
    };
  }

  function informeDetalle() {
    return {
      id: 10,
      numero: 1,
      contratoId: 20,
      contratoNumero: 'OPS-2026-020',
      fechaInicio: '2026-02-01',
      fechaFin: '2026-02-28',
      estado: 'BORRADOR',
      fechaUltimoEnvio: null,
      fechaAprobacion: null,
      contratista: null,
      revisor: null,
      supervisor: null,
      actividades: [],
      documentosAdicionales: [],
      observaciones: []
    };
  }
});
