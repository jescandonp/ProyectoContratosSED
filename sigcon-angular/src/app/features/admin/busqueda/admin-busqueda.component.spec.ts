import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import {
  BusquedaAdminFiltros,
  BusquedaAdminPageResponse,
  BusquedaAdminResponse,
  BusquedaAdminService
} from '../../../core/services/busqueda-admin.service';
import { AdminBusquedaComponent } from './admin-busqueda.component';

describe('AdminBusquedaComponent', () => {
  let fixture: ComponentFixture<AdminBusquedaComponent>;
  let component: AdminBusquedaComponent;
  let busquedaService: jasmine.SpyObj<BusquedaAdminService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    busquedaService = jasmine.createSpyObj<BusquedaAdminService>('BusquedaAdminService', ['buscar', 'buscarAvanzado']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    busquedaService.buscar.and.returnValue(of(respuestaVacia()));
    busquedaService.buscarAvanzado.and.returnValue(of(paginaVacia()));
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

  // ── Tests de formulario ───────────────────────────────────────────────────

  it('renderiza el formulario de búsqueda', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="input-busqueda"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="input-fecha-inicio"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="input-fecha-fin"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="btn-limpiar"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="btn-buscar"]')).not.toBeNull();
  });

  it('renderiza los selectores de estado contrato e informe', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="select-estado-contrato"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="select-estado-informe"]')).not.toBeNull();
  });

  it('no muestra resultados antes de buscar', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratos-avanzada"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratistas"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratos"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-informes"]')).toBeNull();
  });

  // ── Tests de búsqueda avanzada T11 ───────────────────────────────────────

  it('llama a buscarAvanzado con q, fechaInicio y fechaFin al buscar', () => {
    component.termino = 'Ana';
    component.fechaInicio = '2026-05-01';
    component.fechaFin = '2026-05-31';

    component.buscar();

    expect(busquedaService.buscarAvanzado).toHaveBeenCalledWith(jasmine.objectContaining({
      q: 'Ana',
      fechaInicio: '2026-05-01',
      fechaFin: '2026-05-31',
      pagina: 0,
      tamano: 20
    }));
  });

  it('llama a buscarAvanzado con filtros de estado contrato e informe', () => {
    component.termino = '';
    component.estadoContrato = 'EN_EJECUCION';
    component.estadoInforme = 'EN_REVISION';

    component.buscar();

    expect(busquedaService.buscarAvanzado).toHaveBeenCalledWith(jasmine.objectContaining({
      estadoContrato: 'EN_EJECUCION',
      estadoInforme: 'EN_REVISION'
    }));
  });

  it('muestra la sección de contratos avanzada tras buscar', () => {
    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratos-avanzada"]')).not.toBeNull();
  });

  it('renderiza contratos con informes anidados', () => {
    busquedaService.buscarAvanzado.and.returnValue(of({
      contratos: [{
        id: 10,
        numero: 'OPS-2026-001',
        objeto: 'Objeto del contrato',
        estado: 'EN_EJECUCION',
        contratistaNombre: 'Ana García',
        informes: [{
          id: 50, numero: 3, estado: 'EN_REVISION',
          fechaInicio: '2026-05-01', fechaFin: '2026-05-31',
          contratoNumero: 'OPS-2026-001', contratistaNombre: 'Ana García'
        }]
      }],
      totalElementos: 1,
      paginaActual: 0,
      totalPaginas: 1,
      tamano: 20
    }));

    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="resultado-contrato-10"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="resultado-informe-50"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('OPS-2026-001');
    expect(fixture.nativeElement.textContent).toContain('Informe No. 3');
    expect(fixture.nativeElement.textContent).toContain('EN_REVISION');
  });

  it('muestra paginación cuando hay más de una página', () => {
    busquedaService.buscarAvanzado.and.returnValue(of({
      contratos: [],
      totalElementos: 25,
      paginaActual: 0,
      totalPaginas: 2,
      tamano: 20
    }));

    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="paginacion"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="btn-pagina-siguiente"]')).not.toBeNull();
  });

  it('navega a la siguiente página al hacer clic en Siguiente', () => {
    busquedaService.buscarAvanzado.and.returnValue(of({
      contratos: [],
      totalElementos: 25,
      paginaActual: 0,
      totalPaginas: 2,
      tamano: 20
    }));

    component.buscar();
    fixture.detectChanges();

    component.irAPagina(1);

    expect(busquedaService.buscarAvanzado).toHaveBeenCalledWith(jasmine.objectContaining({ pagina: 1 }));
  });

  it('muestra error si la búsqueda avanzada falla', () => {
    busquedaService.buscarAvanzado.and.returnValue(throwError(() => new Error('fail')));

    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="error-busqueda"]')).not.toBeNull();
    expect(component.error()).toContain('No se pudo ejecutar');
    expect(component.buscando()).toBeFalse();
  });

  it('limpia filtros, resultados, error y pagina sin ejecutar una nueva búsqueda', () => {
    component.termino = 'Ana';
    component.fechaInicio = '2026-05-01';
    component.fechaFin = '2026-05-31';
    component.estadoContrato = 'EN_EJECUCION';
    component.estadoInforme = 'EN_REVISION';
    component.paginaActual.set(2);
    component.error.set('Error visible');
    component.resultados.set(respuestaVacia());
    component.resultadosAvanzados.set({
      contratos: [{
        id: 10,
        numero: 'OPS-2026-001',
        objeto: 'Objeto',
        estado: 'EN_EJECUCION',
        contratistaNombre: 'Ana García',
        informes: []
      }],
      totalElementos: 1,
      paginaActual: 2,
      totalPaginas: 3,
      tamano: 20
    });

    busquedaService.buscarAvanzado.calls.reset();

    component.limpiar();
    fixture.detectChanges();

    expect(component.termino).toBe('');
    expect(component.fechaInicio).toBe('');
    expect(component.fechaFin).toBe('');
    expect(component.estadoContrato).toBe('');
    expect(component.estadoInforme).toBe('');
    expect(component.paginaActual()).toBe(0);
    expect(component.error()).toBe('');
    expect(component.resultados()).toBeNull();
    expect(component.resultadosAvanzados()).toBeNull();
    expect(busquedaService.buscarAvanzado).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[data-testid="seccion-contratos-avanzada"]')).toBeNull();
  });

  // ── Tests de navegación ───────────────────────────────────────────────────

  it('navega al detalle del contrato al hacer clic en Ver contrato', () => {
    component.irAContrato(10);
    expect(router.navigate).toHaveBeenCalledWith(['/contratos', 10]);
  });

  it('navega al detalle del informe al hacer clic en Ver informe', () => {
    component.irAInforme(50);
    expect(router.navigate).toHaveBeenCalledWith(['/informes', 50]);
  });

  // ── Tests de compatibilidad T8 (buscar simple) ────────────────────────────

  it('llama al servicio con q, fechaInicio y fechaFin al buscar', () => {
    component.termino = 'Ana';
    component.fechaInicio = '2026-05-01';
    component.fechaFin = '2026-05-31';

    component.buscar();

    expect(busquedaService.buscarAvanzado).toHaveBeenCalledWith(jasmine.objectContaining({
      q: 'Ana',
      fechaInicio: '2026-05-01',
      fechaFin: '2026-05-31'
    }));
  });

  it('llama al servicio sin fechas cuando no se especifican', () => {
    component.termino = 'OPS';

    component.buscar();

    expect(busquedaService.buscarAvanzado).toHaveBeenCalledWith(jasmine.objectContaining({ q: 'OPS' }));
  });

  it('rango de fechas filtra por periodo del informe — envía parámetros correctos', () => {
    component.termino = '';
    component.fechaInicio = '2026-05-01';
    component.fechaFin = '2026-05-31';

    component.buscar();

    expect(busquedaService.buscarAvanzado).toHaveBeenCalledWith(jasmine.objectContaining({
      fechaInicio: '2026-05-01',
      fechaFin: '2026-05-31'
    }));
  });
});

function respuestaVacia(): BusquedaAdminResponse {
  return { contratistas: [], contratos: [], informes: [] };
}

function paginaVacia(): BusquedaAdminPageResponse {
  return { contratos: [], totalElementos: 0, paginaActual: 0, totalPaginas: 0, tamano: 20 };
}
