import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { Usuario } from '../../core/models/usuario.model';
import { UsuarioService } from '../../core/services/usuario.service';
import { PerfilComponent } from './perfil.component';

describe('PerfilComponent', () => {
  let fixture: ComponentFixture<PerfilComponent>;
  let usuarioService: jasmine.SpyObj<UsuarioService>;
  let authService: { currentUser: jasmine.Spy };

  beforeEach(async () => {
    usuarioService = jasmine.createSpyObj<UsuarioService>('UsuarioService', [
      'obtenerPerfilActual',
      'actualizarPerfilActual',
      'actualizarFirmaActual'
    ]);
    authService = { currentUser: jasmine.createSpy('currentUser') };

    await TestBed.configureTestingModule({
      imports: [PerfilComponent],
      providers: [
        { provide: UsuarioService, useValue: usuarioService },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();
  });

  it('shows the signature warning when contractor has no signature', () => {
    const usuario = usuarioActual({ firmaImagen: null });
    authService.currentUser.and.returnValue(usuario);
    usuarioService.obtenerPerfilActual.and.returnValue(of(usuario));

    fixture = TestBed.createComponent(PerfilComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="firma-warning"]')?.textContent)
      .toContain('Para que los informes puedan ser aprobados, debe cargar su firma digital.');
  });

  it('does not show the signature warning when signature is present', () => {
    const usuario = usuarioActual({ firmaImagen: 'firmas/ana.png' });
    authService.currentUser.and.returnValue(usuario);
    usuarioService.obtenerPerfilActual.and.returnValue(of(usuario));

    fixture = TestBed.createComponent(PerfilComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="firma-warning"]')).toBeNull();
  });

  function usuarioActual(overrides: Partial<Usuario>): Usuario {
    return {
      id: 1,
      email: 'ana@example.com',
      nombre: 'Ana Contratista',
      cargo: 'Profesional',
      rol: 'CONTRATISTA',
      firmaImagen: null,
      activo: true,
      ...overrides
    };
  }
});
