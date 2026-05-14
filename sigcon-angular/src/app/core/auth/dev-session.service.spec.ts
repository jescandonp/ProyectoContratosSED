import { TestBed } from '@angular/core/testing';

import { DevSessionService } from './dev-session.service';

describe('DevSessionService', () => {
  let service: DevSessionService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(DevSessionService);
  });

  afterEach(() => localStorage.clear());

  it('stores local-dev sessions with full backend email usernames', () => {
    const session = service.loginAs('CONTRATISTA');

    expect(session.email).toBe('juan.escandon@educacionbogota.edu.co');
    expect(session.rol).toBe('CONTRATISTA');
    expect(service.currentSession()?.email).toBe('juan.escandon@educacionbogota.edu.co');
  });

  it('stores the IVA contractor local-dev session by email', () => {
    const session = service.loginAsEmail('aecheverry@educacionbogota.gov.co');

    expect(session.email).toBe('aecheverry@educacionbogota.gov.co');
    expect(session.nombre).toBe('Alvaro Echeverry Salcedo');
    expect(session.cargo).toBe('Asesor');
    expect(session.rol).toBe('CONTRATISTA');
    expect(service.currentSession()?.email).toBe('aecheverry@educacionbogota.gov.co');
    expect(service.authorizationHeader()).toBe(`Basic ${btoa('aecheverry@educacionbogota.gov.co:contratista123')}`);
  });

  it('builds the HTTP Basic header for the active dev user', () => {
    service.loginAs('ADMIN');

    expect(service.authorizationHeader()).toBe(`Basic ${btoa('admin@educacionbogota.edu.co:admin123')}`);
  });
});
