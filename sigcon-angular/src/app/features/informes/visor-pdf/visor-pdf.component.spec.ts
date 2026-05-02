import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { InformeDetalle } from '../../../core/models/informe.model';
import { InformeService } from '../../../core/services/informe.service';
import { PdfInformeService } from '../../../core/services/pdf-informe.service';
import { VisorPdfComponent } from './visor-pdf.component';

describe('VisorPdfComponent', () => {
  let fixture: ComponentFixture<VisorPdfComponent>;
  let informeService: jasmine.SpyObj<InformeService>;
  let pdfService: jasmine.SpyObj<PdfInformeService>;

  beforeEach(async () => {
    informeService = jasmine.createSpyObj<InformeService>('InformeService', ['obtenerDetalle']);
    pdfService = jasmine.createSpyObj<PdfInformeService>('PdfInformeService', ['descargar']);

    informeService.obtenerDetalle.and.returnValue(of(sampleInformeDetalle()));
    pdfService.descargar.and.returnValue(of(new Blob(['pdf'], { type: 'application/pdf' })));

    await TestBed.configureTestingModule({
      imports: [VisorPdfComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '501' }) } } },
        { provide: InformeService, useValue: informeService },
        { provide: PdfInformeService, useValue: pdfService }
      ]
    }).compileComponents();
  });

  it('shows the download button when the PDF is available', () => {
    fixture = TestBed.createComponent(VisorPdfComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="download-pdf"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Informe No. 7');
  });

  it('shows an institutional message when the PDF is not available', () => {
    pdfService.descargar.and.returnValue(throwError(() => ({ status: 404 })));

    fixture = TestBed.createComponent(VisorPdfComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('El PDF no esta disponible. El informe debe estar en estado APROBADO.');
    expect(fixture.nativeElement.querySelector('[data-testid="download-pdf"]')).toBeNull();
  });

  it('downloads the available PDF blob', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:sigcon-pdf');
    spyOn(URL, 'revokeObjectURL');

    fixture = TestBed.createComponent(VisorPdfComponent);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[data-testid="download-pdf"]').click();

    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:sigcon-pdf');
  });

  function sampleInformeDetalle(): InformeDetalle {
    return {
      id: 501,
      numero: 7,
      contratoId: 10,
      contratoNumero: 'OPS-2026-001',
      fechaInicio: '2026-05-01',
      fechaFin: '2026-05-31',
      estado: 'APROBADO',
      fechaUltimoEnvio: '2026-05-10T10:00:00',
      fechaAprobacion: '2026-05-12T15:00:00',
      pdfRuta: 'pdfs/10/501/informe-7.pdf',
      pdfGeneradoAt: '2026-05-12T15:05:00',
      pdfHash: 'abc123',
      contratista: null,
      revisor: null,
      supervisor: null,
      actividades: [],
      documentosAdicionales: [],
      observaciones: []
    };
  }
});
