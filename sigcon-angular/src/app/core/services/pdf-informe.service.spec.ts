import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PdfInformeService } from './pdf-informe.service';

describe('PdfInformeService', () => {
  let service: PdfInformeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PdfInformeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('downloads the approved report PDF as a blob', () => {
    const pdf = new Blob(['PDF'], { type: 'application/pdf' });

    service.descargar(50).subscribe((response) => {
      expect(response.type).toBe('application/pdf');
    });

    const request = http.expectOne('/api/informes/50/pdf');
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    request.flush(pdf);
  });
});
