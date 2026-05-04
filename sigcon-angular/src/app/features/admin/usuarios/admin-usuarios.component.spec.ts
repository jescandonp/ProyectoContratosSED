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
});
