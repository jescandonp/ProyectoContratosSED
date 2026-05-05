import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ContratoService } from '../../../core/services/contrato.service';
import { ObligacionService } from '../../../core/services/obligacion.service';
import { UsuarioService } from '../../../core/services/usuario.service';
import { AdminContratoFormComponent } from './admin-contrato-form.component';

describe('AdminContratoFormComponent', () => {
  let fixture: ComponentFixture<AdminContratoFormComponent>;
  let component: AdminContratoFormComponent;
  let contratoService: jasmine.SpyObj<ContratoService>;
  let obligacionService: jasmine.SpyObj<ObligacionService>;
  let usuarioService: jasmine.SpyObj<UsuarioService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    contratoService = jasmine.createSpyObj<ContratoService>('ContratoService', [
      'obtenerDetalle',
      'crearContrato',
      'actualizarContrato',
      'actualizarContratoAdmin'
    ]);
    obligacionService = jasmine.createSpyObj<ObligacionService>('ObligacionService', [
      'crear',
      'actualizar',
      'eliminar'
    ]);
    usuarioService = jasmine.createSpyObj<UsuarioService>('UsuarioService', ['listarUsuarios']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    contratoService.obtenerDetalle.and.returnValue(of(contratoDetalle([])));
    contratoService.actualizarContrato.and.returnValue(of(contratoDetalle([])));
    contratoService.actualizarContratoAdmin.and.returnValue(of(contratoDetalle([])));
    obligacionService.crear.and.returnValue(of({ id: 20, descripcion: 'Obligacion nueva', orden: 1 }));
    usuarioService.listarUsuarios.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, size: 100, number: 0, first: true, last: true }));

    await TestBed.configureTestingModule({
      imports: [AdminContratoFormComponent],
      providers: [
        { provide: ContratoService, useValue: contratoService },
        { provide: ObligacionService, useValue: obligacionService },
        { provide: UsuarioService, useValue: usuarioService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminContratoFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('persists new obligations after updating an existing contract', () => {
    component.obligaciones.set([{ descripcion: 'Obligacion nueva', orden: 1 }]);

    component.guardar();

    expect(contratoService.actualizarContratoAdmin).toHaveBeenCalledWith(1, jasmine.objectContaining({ numero: 'CO1.PCCNTR 8504408 - 2025' }));
    expect(obligacionService.crear).toHaveBeenCalledWith(1, { descripcion: 'Obligacion nueva', orden: 1 });
    expect(router.navigate).toHaveBeenCalledWith(['/admin/contratos'], {
      state: { mensaje: 'Contrato actualizado correctamente.' }
    });
  });

  // ── T7: H1 revisor opcional + H2 modo edicion ────────────────────────────

  it('crea contrato sin revisor cuando idRevisor es null', () => {
    // Fixture en modo creacion (sin id en ruta)
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [AdminContratoFormComponent],
      providers: [
        { provide: ContratoService, useValue: contratoService },
        { provide: ObligacionService, useValue: obligacionService },
        { provide: UsuarioService, useValue: usuarioService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } }
      ]
    });
    const fixtureCreacion = TestBed.createComponent(AdminContratoFormComponent);
    const comp = fixtureCreacion.componentInstance;
    contratoService.crearContrato.and.returnValue(of(contratoDetalle([])));
    fixtureCreacion.detectChanges();

    comp.form.numero = 'OPS-2026-NEW';
    comp.form.objeto = 'Objeto nuevo';
    comp.form.valorTotal = 5000000;
    comp.form.fechaInicio = '2026-01-01';
    comp.form.fechaFin = '2026-12-31';
    comp.form.idContratista = 1;
    comp.form.idRevisor = null;
    comp.form.idSupervisor = 2;

    comp.guardar();

    expect(contratoService.crearContrato).toHaveBeenCalledWith(
      jasmine.objectContaining({ idRevisor: null })
    );
  });

  it('usa actualizarContratoAdmin al guardar en modo edicion', () => {
    component.obligaciones.set([]);

    component.guardar();

    expect(contratoService.actualizarContratoAdmin).toHaveBeenCalledWith(
      1,
      jasmine.objectContaining({ numero: 'CO1.PCCNTR 8504408 - 2025' })
    );
    expect(contratoService.crearContrato).not.toHaveBeenCalled();
  });

  it('muestra error inline de numero duplicado al recibir 409', () => {
    contratoService.actualizarContratoAdmin.and.returnValue(
      throwError(() => ({ error: { error: 'NUMERO_CONTRATO_DUPLICADO', mensaje: 'Ya existe' } }))
    );

    component.guardar();

    expect(component.errorNumero()).toBe('Ya existe un contrato con ese número.');
    expect(component.error()).toBe('Ya existe un contrato con ese número.');
  });

  function contratoDetalle(obligaciones: { id: number; descripcion: string; orden: number }[]) {
    return {
      id: 1,
      numero: 'CO1.PCCNTR 8504408 - 2025',
      objeto: 'Objeto contractual',
      tipo: 'OPS' as const,
      estado: 'EN_EJECUCION' as const,
      fechaInicio: '2025-11-06',
      fechaFin: '2026-10-30',
      valorTotal: 11900000,
      contratistaNombre: 'Leonardo Fabio Velandia',
      supervisorNombre: 'Milena del Pilar Sandoval Gomez',
      contratista: { id: 1, email: 'contratista@example.com', nombre: 'Leonardo Fabio Velandia', cargo: 'Contratista', rol: 'CONTRATISTA' as const, firmaImagen: null, activo: true },
      revisor: { id: 3, email: 'revisor@example.com', nombre: 'Jorge Mauricio Barragan', cargo: 'Revisor', rol: 'REVISOR' as const, firmaImagen: null, activo: true },
      supervisor: { id: 2, email: 'supervisor@example.com', nombre: 'Milena del Pilar Sandoval Gomez', cargo: 'Supervisor', rol: 'SUPERVISOR' as const, firmaImagen: null, activo: true },
      obligaciones,
      docsAplicables: []
    };
  }
});
