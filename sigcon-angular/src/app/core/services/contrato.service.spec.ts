import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { ContratoService } from './contrato.service';
import { ContratoRequest } from '../models/contrato.model';

describe('ContratoService', () => {
  let service: ContratoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ContratoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses relative I1 contrato endpoints', () => {
    service.listarContratos({ page: 0, size: 10, estado: 'EN_EJECUCION', search: 'OPS' }).subscribe();
    const listRequest = http.expectOne((request) =>
      request.url === '/api/contratos' &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '10' &&
      request.params.get('estado') === 'EN_EJECUCION' &&
      request.params.get('search') === 'OPS'
    );
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });

    service.obtenerDetalle(15).subscribe();
    expect(http.expectOne('/api/contratos/15').request.method).toBe('GET');

    service.crearContrato(contratoRequest()).subscribe();
    expect(http.expectOne('/api/contratos').request.method).toBe('POST');

    service.cambiarEstado(15, 'CERRADO').subscribe();
    const stateRequest = http.expectOne('/api/contratos/15/estado');
    expect(stateRequest.request.method).toBe('PATCH');
    expect(stateRequest.request.body).toEqual({ estado: 'CERRADO' });
  });

  function contratoRequest(): ContratoRequest {
    return {
      numero: 'OPS-2026-001',
      objeto: 'Objeto contractual',
      tipo: 'OPS',
      valorTotal: 18000000,
      fechaInicio: '2026-01-15',
      fechaFin: '2026-12-31',
      idContratista: 2,
      idRevisor: 3,
      idSupervisor: 4
    };
  }
});
