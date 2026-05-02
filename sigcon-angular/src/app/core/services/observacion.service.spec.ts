import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ObservacionService } from './observacion.service';
import { ObservacionRequest } from '../models/observacion.model';

describe('ObservacionService', () => {
  let service: ObservacionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ObservacionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses informe transition endpoints that carry observaciones', () => {
    const request: ObservacionRequest = { texto: 'Ajustar soporte' };

    service.aprobarRevision(10, { texto: 'Listo para supervision' }).subscribe();
    const approveReviewRequest = http.expectOne('/api/informes/10/aprobar-revision');
    expect(approveReviewRequest.request.method).toBe('POST');
    expect(approveReviewRequest.request.body).toEqual({ texto: 'Listo para supervision' });
    approveReviewRequest.flush(informeDetalle());

    service.aprobarRevision(10).subscribe();
    const approveReviewWithoutBody = http.expectOne('/api/informes/10/aprobar-revision');
    expect(approveReviewWithoutBody.request.method).toBe('POST');
    expect(approveReviewWithoutBody.request.body).toBeNull();
    approveReviewWithoutBody.flush(informeDetalle());

    service.devolverRevision(10, request).subscribe();
    const returnReviewRequest = http.expectOne('/api/informes/10/devolver-revision');
    expect(returnReviewRequest.request.method).toBe('POST');
    expect(returnReviewRequest.request.body).toEqual(request);
    returnReviewRequest.flush(informeDetalle());

    service.devolverInforme(10, request).subscribe();
    const returnFinalRequest = http.expectOne('/api/informes/10/devolver');
    expect(returnFinalRequest.request.method).toBe('POST');
    expect(returnFinalRequest.request.body).toEqual(request);
    returnFinalRequest.flush(informeDetalle());
  });

  function informeDetalle() {
    return {
      id: 10,
      numero: 1,
      contratoId: 20,
      contratoNumero: 'OPS-2026-020',
      fechaInicio: '2026-02-01',
      fechaFin: '2026-02-28',
      estado: 'ENVIADO',
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
