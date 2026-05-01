import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { UsuarioService } from './usuario.service';
import { RolUsuario, UsuarioRequest } from '../models/usuario.model';

describe('UsuarioService', () => {
  let service: UsuarioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses relative I1 usuario endpoints', () => {
    service.obtenerPerfilActual().subscribe();
    http.expectOne('/api/usuarios/me').flush(usuario(1, 'ADMIN'));

    service.listarUsuarios({ page: 0, size: 10, rol: 'CONTRATISTA' }).subscribe();
    const listRequest = http.expectOne((request) =>
      request.url === '/api/usuarios' &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '10' &&
      request.params.get('rol') === 'CONTRATISTA'
    );
    expect(listRequest.request.method).toBe('GET');
    listRequest.flush({ content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 });

    const request: UsuarioRequest = {
      email: 'nuevo@educacionbogota.edu.co',
      nombre: 'Nuevo Usuario',
      cargo: 'Contratista',
      rol: 'CONTRATISTA'
    };
    service.crearUsuario(request).subscribe();
    const createRequest = http.expectOne('/api/usuarios');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(request);
    createRequest.flush(usuario(2, 'CONTRATISTA'));
  });

  it('uploads firma using multipart endpoint', () => {
    const file = new File(['firma'], 'firma.png', { type: 'image/png' });

    service.actualizarFirmaActual(file).subscribe();

    const uploadRequest = http.expectOne('/api/usuarios/me/firma');
    expect(uploadRequest.request.method).toBe('POST');
    expect(uploadRequest.request.body instanceof FormData).toBeTrue();
    uploadRequest.flush(usuario(1, 'ADMIN'));
  });

  function usuario(id: number, rol: RolUsuario) {
    return {
      id,
      email: `usuario${id}@educacionbogota.edu.co`,
      nombre: `Usuario ${id}`,
      cargo: 'Cargo',
      rol,
      firmaImagen: null,
      activo: true
    };
  }
});
