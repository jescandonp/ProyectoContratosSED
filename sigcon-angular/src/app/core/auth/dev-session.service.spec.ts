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

  it('builds the HTTP Basic header for the active dev user', () => {
    service.loginAs('ADMIN');

    expect(service.authorizationHeader()).toBe(`Basic ${btoa('admin@educacionbogota.edu.co:admin123')}`);
  });
});
