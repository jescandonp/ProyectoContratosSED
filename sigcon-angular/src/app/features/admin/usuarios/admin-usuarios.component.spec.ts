import { ComponentFixture, TestBed } from '@angular/core/testing';
import { throwError, of } from 'rxjs';

import { UsuarioService } from '../../../core/services/usuario.service';
import { AdminUsuariosComponent } from './admin-usuarios.component';

describe('AdminUsuariosComponent', () => {
  let fixture: ComponentFixture<AdminUsuariosComponent>;
  let component: AdminUsuariosComponent;
  let usuarioService: jasmine.SpyObj<UsuarioService>;

  beforeEach(async () => {
    usuarioService = jasmine.createSpyObj<UsuarioService>('UsuarioService', [
      'listarUsuarios',
      'crearUsuario',
      'actualizarUsuario',
      'cambiarEstado'
    ]);
    usuarioService.listarUsuarios.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, size: 15, number: 0, first: true, last: true }));

    await TestBed.configureTestingModule({
      imports: [AdminUsuariosComponent],
      providers: [{ provide: UsuarioService, useValue: usuarioService }]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsuariosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows connection guidance when user creation cannot reach backend', () => {
    usuarioService.crearUsuario.and.returnValue(throwError(() => ({ status: 0 })));

    component.abrirFormulario(null);
    component.formUsuario = {
      email: 'jescandon@educacionbogota.gov.co',
      nombre: 'Juan Manuel Escandon',
      cargo: 'Lider PMO',
      rol: 'CONTRATISTA'
    };
    component.guardarUsuario();

    expect(component.errorForm()).toBe('No se pudo conectar con el backend. Verifica que Spring Boot este iniciado en localhost:8080.');
  });

  it('defaults new contractor users to not responsable IVA', () => {
    component.abrirFormulario(null);

    expect(component.formUsuario.responsableIva).toBeFalse();
  });

  it('sends responsable IVA flag when creating user', () => {
    usuarioService.crearUsuario.and.returnValue(of({
      id: 7,
      email: 'contratista@sed.gov.co',
      nombre: 'Contratista Responsable',
      cargo: 'Contratista',
      rol: 'CONTRATISTA',
      firmaImagen: null,
      activo: true,
      sgssiSaludEntidad: null,
      sgssiPensionEntidad: null,
      sgssiArlEntidad: null,
      responsableIva: true,
      esAdmin: true
    }));

    component.abrirFormulario(null);
    component.formUsuario = {
      email: 'contratista@sed.gov.co',
      nombre: 'Contratista Responsable',
      cargo: 'Contratista',
      rol: 'CONTRATISTA',
      responsableIva: true
    };
    component.guardarUsuario();

    expect(usuarioService.crearUsuario).toHaveBeenCalledWith(jasmine.objectContaining({ responsableIva: true }));
  });

  it('sends esAdmin only for contractor users', () => {
    usuarioService.crearUsuario.and.returnValue(of({
      id: 9,
      email: 'dual@sed.gov.co',
      nombre: 'Contratista Admin',
      cargo: 'Contratista',
      rol: 'CONTRATISTA',
      firmaImagen: null,
      activo: true,
      sgssiSaludEntidad: null,
      sgssiPensionEntidad: null,
      sgssiArlEntidad: null,
      responsableIva: false,
      esAdmin: true
    }));

    component.abrirFormulario(null);
    component.formUsuario = {
      email: 'dual@sed.gov.co',
      nombre: 'Contratista Admin',
      cargo: 'Contratista',
      rol: 'CONTRATISTA',
      esAdmin: true
    };
    component.guardarUsuario();

    expect(usuarioService.crearUsuario).toHaveBeenCalledWith(jasmine.objectContaining({ esAdmin: true }));
  });

  it('shows success message after creating user', () => {
    usuarioService.crearUsuario.and.returnValue(of({
      id: 8,
      email: 'nuevo@sed.gov.co',
      nombre: 'Usuario Nuevo',
      cargo: 'Contratista',
      rol: 'CONTRATISTA',
      firmaImagen: null,
      activo: true,
      sgssiSaludEntidad: null,
      sgssiPensionEntidad: null,
      sgssiArlEntidad: null,
      responsableIva: false,
      esAdmin: false
    }));

    component.abrirFormulario(null);
    component.formUsuario = {
      email: 'nuevo@sed.gov.co',
      nombre: 'Usuario Nuevo',
      cargo: 'Contratista',
      rol: 'CONTRATISTA',
      responsableIva: false
    };
    component.guardarUsuario();

    expect(component.mensajeExito()).toBe('Usuario creado correctamente.');
  });
});
