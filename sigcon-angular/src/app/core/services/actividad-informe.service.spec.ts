import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ActividadInformeService } from './actividad-informe.service';
import { ActividadInformeRequest } from '../models/actividad-informe.model';

describe('ActividadInformeService', () => {
  let service: ActividadInformeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ActividadInformeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses relative I2 actividad endpoints nested under informes', () => {
    const request: ActividadInformeRequest = {
      idObligacion: 21,
      descripcion: 'Actividad ejecutada',
      porcentaje: 50
    };

    service.crear(10, request).subscribe();
    const createRequest = http.expectOne('/api/informes/10/actividades');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(request);
    createRequest.flush(actividad());

    service.actualizar(10, 11, request).subscribe();
    const updateRequest = http.expectOne('/api/informes/10/actividades/11');
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(request);
    updateRequest.flush(actividad());

    service.eliminar(10, 11).subscribe();
    expect(http.expectOne('/api/informes/10/actividades/11').request.method).toBe('DELETE');
  });

  function actividad() {
    return {
      id: 11,
      idObligacion: 21,
      ordenObligacion: 1,
      descripcionObligacion: 'Obligacion',
      descripcion: 'Actividad ejecutada',
      porcentaje: 50,
      soportes: []
    };
  }
});
