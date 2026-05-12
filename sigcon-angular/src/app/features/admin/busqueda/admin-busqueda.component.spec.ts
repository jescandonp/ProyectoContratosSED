import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { BusquedaAdminResponse, BusquedaAdminService } from '../../../core/services/busqueda-admin.service';
import { AdminBusquedaComponent } from './admin-busqueda.component';

describe('AdminBusquedaComponent', () => {
  let fixture: ComponentFixture<AdminBusquedaComponent>;
  let component: AdminBusquedaComponent;
  let busquedaService: jasmine.SpyObj<BusquedaAdminService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    busquedaService = jasmine.createSpyObj<BusquedaAdminService>('BusquedaAdminService', ['buscar']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    busquedaService.buscar.and.returnValue(of(respuestaVacia()));
    router.navigate.and.returnValue(Promise.resolve(true));

    await TestBed.configureTestingModule({
      imports: [AdminBusquedaComponent],
      providers: [
        { provide: BusquedaAdminService, useValue: busquedaService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminBusquedaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renderiza el formulario de búsqueda', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="input-busqueda"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="input-fecha-inicio"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="input-fecha-fin"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="btn-buscar"]')).not.toBeNull();
  });

  it('no muestra resultados antes de buscar', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratistas"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratos"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-informes"]')).toBeNull();
  });

  it('llama al servicio con q, fechaInicio y fechaFin al buscar', () => {
    component.termino = 'Ana';
    component.fechaInicio = '2026-05-01';
    component.fechaFin = '2026-05-31';

    component.buscar();

    expect(busquedaService.buscar).toHaveBeenCalledWith('Ana', '2026-05-01', '2026-05-31');
  });

  it('llama al servicio sin fechas cuando no se especifican', () => {
    component.termino = 'OPS';

    component.buscar();

    expect(busquedaService.buscar).toHaveBeenCalledWith('OPS', undefined, undefined);
  });

  it('muestra las tres secciones de resultados tras buscar', () => {
    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratistas"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratos"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-informes"]')).not.toBeNull();
  });

  it('renderiza contratistas encontrados', () => {
    busquedaService.buscar.and.returnValue(of({
      contratistas: [{ id: 1, nombre: 'Ana García', email: 'ana@sed.gov.co', cargo: 'Profesional' }],
      contratos: [],
      informes: []
    }));

    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="resultado-contratista-1"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Ana García');
    expect(fixture.nativeElement.textContent).toContain('ana@sed.gov.co');
  });

  it('renderiza contratos encontrados', () => {
    busquedaService.buscar.and.returnValue(of({
      contratistas: [],
      contratos: [{ id: 10, numero: 'OPS-2026-001', objeto: 'Objeto del contrato', estado: 'EN_EJECUCION', contratistaNombre: 'Ana García' }],
      informes: []
    }));

    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="resultado-contrato-10"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('OPS-2026-001');
  });

  it('renderiza informes encontrados', () => {
    busquedaService.buscar.and.returnValue(of({
      contratistas: [],
      contratos: [],
      informes: [{
        id: 50, numero: 3, estado: 'APROBADO',
        fechaInicio: '2026-05-01', fechaFin: '2026-05-31',
        contratoNumero: 'OPS-2026-001', contratistaNombre: 'Ana García'
      }]
    }));

    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="resultado-informe-50"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Informe No. 3');
    expect(fixture.nativeElement.textContent).toContain('APROBADO');
  });

  it('muestra error si la búsqueda falla', () => {
    busquedaService.buscar.and.returnValue(throwError(() => new Error('fail')));

    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="error-busqueda"]')).not.toBeNull();
    expect(component.error()).toContain('No se pudo ejecutar');
    expect(component.buscando()).toBeFalse();
  });

  it('navega al detalle del contrato al hacer clic en Ver detalle', () => {
    busquedaService.buscar.and.returnValue(of({
      contratistas: [],
      contratos: [{ id: 10, numero: 'OPS-2026-001', objeto: 'Objeto', estado: 'EN_EJECUCION', contratistaNombre: null }],
      informes: []
    }));

    component.buscar();
    fixture.detectChanges();

    component.irAContrato(10);

    expect(router.navigate).toHaveBeenCalledWith(['/contratos', 10]);
  });

  it('navega al detalle del informe al hacer clic en Ver detalle', () => {
    component.irAInforme(50);

    expect(router.navigate).toHaveBeenCalledWith(['/informes', 50]);
  });

  it('rango de fechas filtra por periodo del informe — envía parámetros correctos', () => {
    component.termino = '';
    component.fechaInicio = '2026-05-01';
    component.fechaFin = '2026-05-31';

    component.buscar();

    expect(busquedaService.buscar).toHaveBeenCalledWith('', '2026-05-01', '2026-05-31');
  });
});

function respuestaVacia(): BusquedaAdminResponse {
  return { contratistas: [], contratos: [], informes: [] };
}
